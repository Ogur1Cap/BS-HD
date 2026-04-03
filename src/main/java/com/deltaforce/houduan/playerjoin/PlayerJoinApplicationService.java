package com.deltaforce.houduan.playerjoin;

import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.notification.NotificationEntity;
import com.deltaforce.houduan.notification.NotificationRepository;
import com.deltaforce.houduan.player.PlayerEntity;
import com.deltaforce.houduan.player.PlayerRepository;
import com.deltaforce.houduan.user.UserEntity;
import com.deltaforce.houduan.user.UserRepository;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlayerJoinApplicationService {
    private static final String DEFAULT_AVATAR = "https://picsum.photos/id/1005/300/300";

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PlayerJoinApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;

    public PlayerJoinApplicationService(UserRepository userRepository,
                                        PlayerRepository playerRepository,
                                        PlayerJoinApplicationRepository applicationRepository,
                                        NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
        this.applicationRepository = applicationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Map<String, Object> submit(Long userId, SubmitJoinBody body) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new BizException(404, "用户不存在"));
        if (user.getUserLevel() > 0) {
            throw new BizException(400, "您已是打手或管理员，无需重复申请");
        }
        if (applicationRepository.existsByUserIdAndStatus(userId, JoinApplicationStatus.PENDING)) {
            throw new BizException(400, "您已有待审核的申请，请耐心等待");
        }
        String displayName = body.getDisplayName() == null ? "" : body.getDisplayName().trim();
        String intro = body.getIntro() == null ? "" : body.getIntro().trim();
        if (displayName.length() < 2 || displayName.length() > 100) {
            throw new BizException(400, "展示昵称长度为 2–100 字");
        }
        if (intro.length() < 10 || intro.length() > 500) {
            throw new BizException(400, "自我介绍长度为 10–500 字");
        }
        LocalDateTime now = LocalDateTime.now();
        PlayerJoinApplicationEntity e = new PlayerJoinApplicationEntity();
        e.setUserId(userId);
        e.setStatus(JoinApplicationStatus.PENDING);
        e.setDisplayName(displayName);
        e.setIntro(intro);
        e.setSkills(trimToNull(body.getSkills(), 255));
        e.setRankName(trimToNull(body.getRankName(), 50));
        if (e.getRankName() == null) {
            e.setRankName("铂金");
        }
        e.setTags(trimToNull(body.getTags(), 255));
        e.setPricePerHour(body.getPricePerHour() == null ? new BigDecimal("99.00") : body.getPricePerHour());
        e.setContactNote(trimToNull(body.getContactNote(), 255));
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        applicationRepository.save(e);

        List<UserEntity> bosses = userRepository.findByUserLevel(2);
        for (UserEntity b : bosses) {
            notify(b.getId(), "新的打手入驻申请",
                    String.format("用户 %s 提交了「加入我们」申请，昵称：%s，请及时审核。", user.getUsername(), displayName),
                    String.valueOf(e.getId()), "boss");
        }

        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("message", "申请已提交，请等待平台审核");
        res.put("applicationId", e.getId());
        return res;
    }

    public Map<String, Object> getMy(Long userId) {
        return applicationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toDto)
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("hasApplication", true);
                    m.put("application", d);
                    return m;
                })
                .orElseGet(() -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("hasApplication", false);
                    m.put("application", null);
                    return m;
                });
    }

    public List<Map<String, Object>> listPendingForBoss(Long bossUserId) {
        requireBoss(bossUserId);
        return applicationRepository.findByStatusOrderByCreatedAtDesc(JoinApplicationStatus.PENDING).stream()
                .map(this::toBossRow)
                .toList();
    }

    @Transactional
    public void approveForBoss(Long bossUserId, Long applicationId) {
        requireBoss(bossUserId);
        PlayerJoinApplicationEntity app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BizException(404, "申请不存在"));
        if (app.getStatus() != JoinApplicationStatus.PENDING) {
            throw new BizException(400, "该申请已处理");
        }
        UserEntity user = userRepository.findById(app.getUserId())
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        if (user.getUserLevel() > 0) {
            throw new BizException(400, "该用户已不是顾客，无法通过入驻");
        }

        PlayerEntity p = new PlayerEntity();
        p.setName(app.getDisplayName());
        p.setAvatar(DEFAULT_AVATAR);
        p.setRankName(app.getRankName());
        p.setSkills(app.getSkills() == null ? "综合护航" : app.getSkills());
        p.setWinRate(new BigDecimal("75"));
        p.setCompletedOrders(0);
        p.setRating(new BigDecimal("4.60"));
        p.setPricePerHour(app.getPricePerHour() == null ? new BigDecimal("99") : app.getPricePerHour());
        p.setIntro(app.getIntro());
        p.setTags(app.getTags() == null ? "平台认证" : app.getTags());
        p.setShowInHall(true);
        playerRepository.save(p);

        user.setUserLevel(1);
        user.setPlayerProfileId(p.getId());
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        app.setStatus(JoinApplicationStatus.APPROVED);
        app.setReviewedAt(now);
        app.setReviewerUserId(bossUserId);
        app.setUpdatedAt(now);
        applicationRepository.save(app);

        notify(user.getId(), "打手申请已通过",
                "恭喜！您的「加入我们」申请已通过，已在大厅展示您的资料。请重新登录以使用打手工作台。",
                String.valueOf(p.getId()), "system");
    }

    @Transactional
    public void rejectForBoss(Long bossUserId, Long applicationId, String reason) {
        requireBoss(bossUserId);
        PlayerJoinApplicationEntity app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BizException(404, "申请不存在"));
        if (app.getStatus() != JoinApplicationStatus.PENDING) {
            throw new BizException(400, "该申请已处理");
        }
        String r = reason == null || reason.isBlank() ? "未说明" : reason.trim();
        LocalDateTime now = LocalDateTime.now();
        app.setStatus(JoinApplicationStatus.REJECTED);
        app.setRejectReason(r);
        app.setReviewedAt(now);
        app.setReviewerUserId(bossUserId);
        app.setUpdatedAt(now);
        applicationRepository.save(app);

        notify(app.getUserId(), "打手申请未通过",
                "您的「加入我们」申请未通过。说明：" + r + "。您可完善资料后再次提交。",
                String.valueOf(app.getId()), "system");
    }

    public List<Map<String, Object>> listPlayerAccountsForBoss(Long bossUserId) {
        requireBoss(bossUserId);
        return userRepository.findAll().stream()
                .filter(u -> u.getUserLevel() == 1)
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", String.valueOf(u.getId()));
                    m.put("username", u.getUsername());
                    m.put("email", u.getEmail());
                    m.put("playerProfileId", u.getPlayerProfileId() == null ? null : String.valueOf(u.getPlayerProfileId()));
                    String pname = "";
                    if (u.getPlayerProfileId() != null) {
                        pname = playerRepository.findById(u.getPlayerProfileId())
                                .map(PlayerEntity::getName)
                                .orElse("");
                    }
                    m.put("playerName", pname);
                    return m;
                })
                .toList();
    }

    @Transactional
    public void revokePlayerForBoss(Long bossUserId, Long targetUserId) {
        requireBoss(bossUserId);
        if (bossUserId.equals(targetUserId)) {
            throw new BizException(400, "不能解除自己的账号");
        }
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        if (target.getUserLevel() >= 2) {
            throw new BizException(400, "不能对 BOSS 账号执行此操作");
        }
        if (target.getUserLevel() != 1) {
            throw new BizException(400, "该用户不是打手账号");
        }
        Long pid = target.getPlayerProfileId();
        target.setUserLevel(0);
        target.setPlayerProfileId(null);
        userRepository.save(target);

        if (pid != null) {
            playerRepository.findById(pid).ifPresent(pl -> {
                pl.setShowInHall(false);
                playerRepository.save(pl);
            });
        }

        notify(targetUserId, "打手资格已解除",
                "您的打手权限已被平台收回，账号已恢复为普通顾客。大厅将不再展示您的打手资料。",
                String.valueOf(targetUserId), "system");
    }

    private void requireBoss(Long userId) {
        UserEntity u = userRepository.findById(userId).orElseThrow(() -> new BizException(404, "用户不存在"));
        if (u.getUserLevel() < 2) {
            throw new BizException(403, "需要 BOSS 账号");
        }
    }

    private Map<String, Object> toDto(PlayerJoinApplicationEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(e.getId()));
        m.put("status", e.getStatus().name());
        m.put("displayName", e.getDisplayName());
        m.put("intro", e.getIntro());
        m.put("skills", e.getSkills());
        m.put("rankName", e.getRankName());
        m.put("tags", e.getTags());
        m.put("pricePerHour", e.getPricePerHour());
        m.put("contactNote", e.getContactNote());
        m.put("createdAt", e.getCreatedAt().toString());
        m.put("reviewedAt", e.getReviewedAt() == null ? null : e.getReviewedAt().toString());
        m.put("rejectReason", e.getRejectReason());
        return m;
    }

    private Map<String, Object> toBossRow(PlayerJoinApplicationEntity e) {
        Map<String, Object> m = toDto(e);
        userRepository.findById(e.getUserId()).ifPresent(u -> {
            m.put("applicantUsername", u.getUsername());
            m.put("applicantEmail", u.getEmail());
        });
        return m;
    }

    private static String trimToNull(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    private void notify(Long uid, String title, String content, String relatedId, String type) {
        NotificationEntity n = new NotificationEntity();
        n.setUserId(uid);
        n.setTitle(title);
        n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        n.setType(type);
        n.setRelatedId(relatedId);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    @Data
    public static class SubmitJoinBody {
        private String displayName;
        private String intro;
        private String skills;
        private String rankName;
        private String tags;
        private BigDecimal pricePerHour;
        private String contactNote;
    }
}
