package com.deltaforce.houduan.violation;

import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.notification.NotificationEntity;
import com.deltaforce.houduan.notification.NotificationRepository;
import com.deltaforce.houduan.user.UserEntity;
import com.deltaforce.houduan.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ViolationService {
    private final ViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private static final int WARNING_THRESHOLD = 3;
    private static final int RESTRICT_THRESHOLD = 5;

    public ViolationService(ViolationRepository violationRepository,
                            UserRepository userRepository,
                            NotificationRepository notificationRepository) {
        this.violationRepository = violationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * 自动记录违规行为
     */
    @Transactional
    public void recordViolation(Long userId, String type, String description, String relatedId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        ViolationEntity violation = new ViolationEntity();
        violation.setUserId(userId);
        violation.setType(type);
        violation.setDescription(description);
        violation.setRelatedId(relatedId);
        violation.setCreatedAt(LocalDateTime.now());
        violation.setUpdatedAt(LocalDateTime.now());
        violationRepository.save(violation);

    user.setViolationCount(user.getViolationCount() + 1);
        
        // 自动风险升级逻辑
        if (user.getViolationCount() >= RESTRICT_THRESHOLD) {
            user.setHighRisk(true);
            user.setStatus("RESTRICTED");
            notifyUser(userId, "账号功能受限", "由于您多次违反平台规定，账号已被限制功能。请联系客服或在违规记录中申诉。", violation.getId());
            notifyBosses("系统预警：账号高频违规", String.format("用户 %s (ID:%d) 违规次数达到 %d 次，系统已自动限制其账号功能。", user.getUsername(), userId, user.getViolationCount()), String.valueOf(violation.getId()));
        } else if (user.getViolationCount() >= WARNING_THRESHOLD && !user.isHighRisk()) {
            user.setHighRisk(true);
            notifyUser(userId, "账号高风险预警", "系统检测到您的账号存在多次违规行为，已标记为高风险。如继续违规将面临限制或封号处理。", violation.getId());
            notifyBosses("系统预警：账号风险升级", String.format("用户 %s (ID:%d) 违规次数达到 %d 次，已被标记为高风险。", user.getUsername(), userId, user.getViolationCount()), String.valueOf(violation.getId()));
        } else {
            notifyUser(userId, "违规警告通知", String.format("系统检测到您存在违规行为：%s。请遵守平台规则。", description), violation.getId());
        }

        userRepository.save(user);
    }

    /**
     * 检查用户是否被禁用或受限
     */
    public void checkUserStatus(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        
        if ("BANNED".equals(user.getStatus())) {
            throw new BizException(403, "您的账号已被封禁，如有疑问请联系客服");
        }
        if ("RESTRICTED".equals(user.getStatus())) {
            throw new BizException(403, "您的账号功能已被限制，无法进行此操作");
        }
    }

    /**
     * 用户提交申诉
     */
    @Transactional
    public void submitAppeal(Long userId, Long violationId, String appealReason) {
        ViolationEntity violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new BizException(404, "违规记录不存在"));
        
        if (!violation.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作此记录");
        }
        
        if (!"PENDING".equals(violation.getStatus())) {
            throw new BizException(400, "该违规记录当前状态不可申诉");
        }

        if (appealReason == null || appealReason.trim().isEmpty()) {
            throw new BizException(400, "申诉理由不能为空");
        }

        violation.setStatus("APPEALED");
        violation.setAppealReason(appealReason.trim());
        violation.setUpdatedAt(LocalDateTime.now());
        violationRepository.save(violation);

        notifyBosses("收到违规申诉", String.format("用户提交了对违规记录 #%d 的申诉，请及时处理。", violationId), String.valueOf(violationId));
    }

    /**
     * BOSS获取待处理违规/预警列表
     */
    public List<Map<String, Object>> listPendingViolations(Long bossId) {
        return violationRepository.findByStatusOrderByCreatedAtDesc("PENDING").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * BOSS获取申诉列表
     */
    public List<Map<String, Object>> listAppealedViolations(Long bossId) {
        return violationRepository.findByStatusOrderByCreatedAtDesc("APPEALED").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * BOSS获取所有违规记录
     */
    public List<Map<String, Object>> listAllViolations(Long bossId) {
        return violationRepository.findByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 用户获取自己的违规记录
     */
    public List<Map<String, Object>> listUserViolations(Long userId) {
        return violationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * BOSS处理违规或申诉
     */
    @Transactional
    public void handleViolation(Long bossId, Long violationId, String action, String notes) {
        ViolationEntity violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new BizException(404, "违规记录不存在"));
        
        UserEntity user = userRepository.findById(violation.getUserId())
                .orElseThrow(() -> new BizException(404, "违规用户不存在"));

        violation.setStatus("RESOLVED");
        violation.setAdminId(bossId);
        violation.setAdminAction(action);
        violation.setAdminNotes(notes);
        violation.setUpdatedAt(LocalDateTime.now());
        
        switch (action) {
            case "WARNING":
                notifyUser(user.getId(), "违规处理结果通知", String.format("关于您的违规记录，平台已给出警告处理。处理意见：%s", notes), violation.getId());
                break;
            case "RESTRICT":
                user.setStatus("RESTRICTED");
                notifyUser(user.getId(), "账号限制通知", String.format("关于您的违规记录，平台已限制您的账号功能。处理意见：%s", notes), violation.getId());
                break;
            case "BAN":
                user.setStatus("BANNED");
                notifyUser(user.getId(), "账号封禁通知", String.format("关于您的违规记录，平台已封禁您的账号。处理意见：%s", notes), violation.getId());
                break;
            case "DISMISS":
                // 撤销违规，减少违规次数
                if (user.getViolationCount() > 0) {
                    user.setViolationCount(user.getViolationCount() - 1);
                }
                if (user.getViolationCount() < WARNING_THRESHOLD) {
                    user.setHighRisk(false);
                }
                if ("RESTRICTED".equals(user.getStatus()) && user.getViolationCount() < RESTRICT_THRESHOLD) {
                    user.setStatus("ACTIVE"); // 满足条件恢复活跃
                }
                notifyUser(user.getId(), "申诉通过通知", String.format("您的违规申诉已通过，平台已撤销该条违规记录。处理意见：%s", notes), violation.getId());
                break;
            default:
                throw new BizException(400, "无效的处理操作");
        }

        userRepository.save(user);
        violationRepository.save(violation);
    }

    private void notifyUser(Long userId, String title, String content, Long violationId) {
        NotificationEntity n = new NotificationEntity();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        n.setType("violation");
        n.setRelatedId(String.valueOf(violationId));
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    private void notifyBosses(String title, String content, String relatedId) {
        List<UserEntity> bosses = userRepository.findByUserLevel(2);
        for (UserEntity b : bosses) {
            NotificationEntity n = new NotificationEntity();
            n.setUserId(b.getId());
            n.setTitle(title);
            n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
            n.setType("boss_violation");
            n.setRelatedId(relatedId);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    private Map<String, Object> toDto(ViolationEntity v) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", String.valueOf(v.getId()));
        dto.put("userId", String.valueOf(v.getUserId()));
        
        userRepository.findById(v.getUserId()).ifPresent(u -> {
            dto.put("username", u.getUsername());
            dto.put("violationCount", u.getViolationCount());
            dto.put("isHighRisk", u.isHighRisk());
            dto.put("userStatus", u.getStatus());
        });

        dto.put("type", v.getType());
        dto.put("description", v.getDescription());
        dto.put("relatedId", v.getRelatedId());
        dto.put("status", v.getStatus());
        dto.put("appealReason", v.getAppealReason());
        dto.put("adminAction", v.getAdminAction());
        dto.put("adminNotes", v.getAdminNotes());
        dto.put("createdAt", v.getCreatedAt().toString());
        if (v.getUpdatedAt() != null) {
            dto.put("updatedAt", v.getUpdatedAt().toString());
        }
        return dto;
    }
}