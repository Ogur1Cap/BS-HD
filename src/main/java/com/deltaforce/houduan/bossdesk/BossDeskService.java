package com.deltaforce.houduan.bossdesk;

import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.notification.NotificationEntity;
import com.deltaforce.houduan.notification.NotificationRepository;
import com.deltaforce.houduan.order.*;
import com.deltaforce.houduan.player.PlayerEntity;
import com.deltaforce.houduan.player.PlayerRepository;
import com.deltaforce.houduan.playerjoin.JoinApplicationStatus;
import com.deltaforce.houduan.playerjoin.PlayerJoinApplicationRepository;
import com.deltaforce.houduan.user.UserEntity;
import com.deltaforce.houduan.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BossDeskService {
    private static final EnumSet<OrderStatus> MANAGEABLE = EnumSet.of(
            OrderStatus.PENDING, OrderStatus.IN_PROGRESS, OrderStatus.COMPLETION_PENDING);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderOperationRepository operationRepository;
    private final NotificationRepository notificationRepository;
    private final PlayerRepository playerRepository;
    private final PlayerJoinApplicationRepository joinApplicationRepository;

    public BossDeskService(UserRepository userRepository,
                           OrderRepository orderRepository,
                           OrderOperationRepository operationRepository,
                           NotificationRepository notificationRepository,
                           PlayerRepository playerRepository,
                           PlayerJoinApplicationRepository joinApplicationRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.operationRepository = operationRepository;
        this.notificationRepository = notificationRepository;
        this.playerRepository = playerRepository;
        this.joinApplicationRepository = joinApplicationRepository;
    }

    public Map<String, Object> stats(Long bossUserId) {
        requireBoss(bossUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("pendingCompletionCount",
                orderRepository.findByStatusOrderByUpdatedAtDesc(OrderStatus.COMPLETION_PENDING).size());
        data.put("manageableOrderCount",
                orderRepository.findByStatusInOrderByUpdatedAtDesc(new ArrayList<>(MANAGEABLE)).size());
        data.put("pendingJoinCount", joinApplicationRepository.countByStatus(JoinApplicationStatus.PENDING));
        data.put("displayHint", "审核打手完成申请时请核对履约情况；转派订单务必填写备注，顾客将收到通知。");
        return data;
    }

    public List<Map<String, Object>> listCompletionPending(Long bossUserId) {
        requireBoss(bossUserId);
        return orderRepository.findByStatusOrderByUpdatedAtDesc(OrderStatus.COMPLETION_PENDING).stream()
                .map(this::toBossOrderDto)
                .toList();
    }

    public List<Map<String, Object>> listManageableOrders(Long bossUserId) {
        requireBoss(bossUserId);
        return orderRepository.findByStatusInOrderByUpdatedAtDesc(new ArrayList<>(MANAGEABLE)).stream()
                .map(this::toBossOrderDto)
                .toList();
    }

    public List<Map<String, Object>> listPlayersForReassign(Long bossUserId) {
        requireBoss(bossUserId);
        return playerRepository.findByShowInHallIsTrueOrderByIdAsc().stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("avatar", p.getAvatar());
                    return m;
                })
                .toList();
    }

    @Transactional
    public void approveCompletion(Long bossUserId, Long orderId) {
        requireBoss(bossUserId);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (order.getStatus() != OrderStatus.COMPLETION_PENDING) {
            throw new BizException(400, "当前状态不可审核完成");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletionRequestNote(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        logOp(orderId, bossUserId, "BOSS_COMPLETION_APPROVE", "approved");
        notifyCustomer(order.getUserId(), "订单已完成",
                String.format("平台已确认订单 #%s（%s）已完成，感谢您的信任。", orderId, order.getServiceType()),
                String.valueOf(orderId));
        Long playerPid = parsePlayerId(order.getPlayerId());
        if (playerPid != null) {
            notifyPlayerAccounts(playerPid, "完成申请已通过",
                    String.format("您申请的订单 #%s 已完成审核，订单已标记为完成。", orderId),
                    String.valueOf(orderId));
        }
    }

    @Transactional
    public void rejectCompletion(Long bossUserId, Long orderId, String reason) {
        requireBoss(bossUserId);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (order.getStatus() != OrderStatus.COMPLETION_PENDING) {
            throw new BizException(400, "当前状态不可驳回完成申请");
        }
        String r = reason == null || reason.isBlank() ? "未说明" : reason.trim();
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setCompletionRequestNote(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        logOp(orderId, bossUserId, "BOSS_COMPLETION_REJECT", truncate(r, 250));
        notifyCustomer(order.getUserId(), "订单完成申请未通过",
                String.format("平台暂未确认订单 #%s 完成。说明：%s。服务将继续由当前打手进行，如有疑问请联系客服。",
                        orderId, r),
                String.valueOf(orderId));
        Long playerPid = parsePlayerId(order.getPlayerId());
        if (playerPid != null) {
            notifyPlayerAccounts(playerPid, "完成申请被驳回",
                    String.format("订单 #%s 的完成申请未通过。原因：%s", orderId, r),
                    String.valueOf(orderId));
        }
    }

    @Transactional
    public void reassignOrder(Long bossUserId, Long orderId, ReassignOrderBody body) {
        requireBoss(bossUserId);
        if (body == null || body.getTargetPlayerId() == null) {
            throw new BizException(400, "缺少 targetPlayerId");
        }
        long targetPlayerId = body.getTargetPlayerId();
        if (body.getRemark() == null || body.getRemark().isBlank()) {
            throw new BizException(400, "转派必须填写备注（将通知顾客）");
        }
        String rem = body.getRemark().trim();
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!MANAGEABLE.contains(order.getStatus())) {
            throw new BizException(400, "当前订单状态不可转派");
        }
        PlayerEntity target = playerRepository.findById(targetPlayerId)
                .orElseThrow(() -> new BizException(404, "目标打手不存在"));

        String oldPlayerId = order.getPlayerId();
        String oldName = order.getPlayerName();

        order.setPlayerId(String.valueOf(target.getId()));
        order.setPlayerName(target.getName());
        if (order.getStatus() == OrderStatus.COMPLETION_PENDING) {
            order.setCompletionRequestNote(null);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            order.setStatus(OrderStatus.IN_PROGRESS);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        String payload = "toPlayer=" + target.getId() + "|" + truncate(rem, 200);
        logOp(orderId, bossUserId, "BOSS_REASSIGN", payload);

        notifyCustomer(order.getUserId(), "订单打手已调整",
                String.format("订单 #%s（%s）已由平台调整承接打手：%s。说明：%s",
                        orderId, order.getServiceType(), target.getName(), rem),
                String.valueOf(orderId));

        Long oldPid = parsePlayerId(oldPlayerId);
        if (oldPid != null && oldPid != target.getId()) {
            notifyPlayerAccounts(oldPid, "订单已转派",
                    String.format("订单 #%s 已由平台转派给其他打手，您无需继续跟进。", orderId),
                    String.valueOf(orderId));
        }
        notifyPlayerAccounts(target.getId(), "BOSS 指派订单",
                String.format("平台将订单 #%s（%s）安排由您承接，请及时在「打手工作台」查看。备注：%s",
                        orderId, order.getServiceType(), rem),
                String.valueOf(orderId));
    }

    private void requireBoss(Long userId) {
        UserEntity u = userRepository.findById(userId).orElseThrow(() -> new BizException(404, "用户不存在"));
        if (u.getUserLevel() < 2) {
            throw new BizException(403, "需要 BOSS 账号（2 级）");
        }
    }

    private Map<String, Object> toBossOrderDto(OrderEntity o) {
        String customerName = userRepository.findById(o.getUserId())
                .map(UserEntity::getUsername)
                .orElse("顾客");
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", String.valueOf(o.getId()));
        dto.put("userId", String.valueOf(o.getUserId()));
        dto.put("customerUsername", customerName);
        dto.put("game", o.getGame());
        dto.put("gameKey", o.getGameKey());
        dto.put("gameImage", o.getGameImage());
        dto.put("serviceType", o.getServiceType());
        dto.put("status", o.getStatus().name());
        dto.put("statusText", statusText(o.getStatus()));
        dto.put("amount", o.getAmount());
        dto.put("createdAt", o.getCreatedAt().toString());
        dto.put("startTime", o.getStartTime() == null ? null : o.getStartTime().toString());
        dto.put("completionRequestNote", o.getCompletionRequestNote());
        Map<String, Object> player = new HashMap<>();
        player.put("id", o.getPlayerId());
        player.put("name", o.getPlayerName());
        dto.put("player", player);
        return dto;
    }

    private static String statusText(OrderStatus status) {
        return switch (status) {
            case PENDING -> "待接单";
            case IN_PROGRESS -> "进行中";
            case COMPLETION_PENDING -> "待审核完成";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case REFUND_REQUESTED -> "退款申请中";
            case REFUNDED -> "已退款";
        };
    }

    private void logOp(Long orderId, Long operatorUserId, String type, String payload) {
        OrderOperationEntity operation = new OrderOperationEntity();
        operation.setOrderId(orderId);
        operation.setUserId(operatorUserId);
        operation.setOperationType(type);
        operation.setPayload(payload.length() > 250 ? payload.substring(0, 250) : payload);
        operation.setCreatedAt(LocalDateTime.now());
        operationRepository.save(operation);
    }

    private void notifyCustomer(Long customerUserId, String title, String content, String relatedId) {
        NotificationEntity n = new NotificationEntity();
        n.setUserId(customerUserId);
        n.setTitle(title);
        n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        n.setType("order");
        n.setRelatedId(relatedId);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    private void notifyPlayerAccounts(Long playerProfileId, String title, String content, String relatedId) {
        List<UserEntity> users = userRepository.findByPlayerProfileId(playerProfileId);
        for (UserEntity u : users) {
            NotificationEntity n = new NotificationEntity();
            n.setUserId(u.getId());
            n.setTitle(title);
            n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
            n.setType("order");
            n.setRelatedId(relatedId);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    private static Long parsePlayerId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
