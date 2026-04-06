package com.deltaforce.houduan.order;

import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.notification.NotificationEntity;
import com.deltaforce.houduan.notification.NotificationRepository;
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

import com.deltaforce.houduan.violation.ViolationService;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderOperationRepository operationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ViolationService violationService;

    public OrderService(OrderRepository orderRepository,
                        OrderOperationRepository operationRepository,
                        UserRepository userRepository,
                        NotificationRepository notificationRepository,
                        ViolationService violationService) {
        this.orderRepository = orderRepository;
        this.operationRepository = operationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.violationService = violationService;
    }

    public List<Map<String, Object>> list(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toOrderDto).toList();
    }

    public Map<String, Object> detail(Long userId, Long orderId) {
        OrderEntity entity = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        return toOrderDto(entity);
    }

    @Transactional
    public List<Map<String, Object>> create(Long userId, CreateOrderRequest request) {
        violationService.checkUserStatus(userId);
        
        // 自动监测：短时间内频繁下单或虚假下单
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentOrdersCount = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getCreatedAt().isAfter(oneHourAgo))
                .count();
        if (recentOrdersCount >= 5) {
            violationService.recordViolation(userId, "FAKE_ORDER", "系统检测到您在1小时内频繁创建超过5个订单，疑似虚假下单恶意刷单。", null);
        }

        OrderEntity entity = new OrderEntity();
        entity.setUserId(userId);
        entity.setGame(request.getGame());
        entity.setGameKey(request.getGameKey());
        entity.setGameImage(request.getGameImage());
        entity.setServiceType(request.getServiceType());
        entity.setAmount(request.getAmount());
        entity.setPlayerId(normalizePlayerIdForStorage(request.getPlayerId()));
        entity.setPlayerName(request.getPlayerName());
        entity.setStatus(OrderStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(entity);
        addOperation(entity.getId(), userId, "CREATE", "created");
        notifyAssignedPlayerUsers(entity);
        return list(userId);
    }

    @Transactional
    public List<Map<String, Object>> cancel(Long userId, Long orderId) {
        OrderEntity entity = mustFind(userId, orderId);
        entity.setStatus(OrderStatus.CANCELLED);
        entity.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(entity);
        addOperation(orderId, userId, "CANCEL", "cancelled");
        return list(userId);
    }

    @Transactional
    public List<Map<String, Object>> reschedule(Long userId, Long orderId, String startTime) {
        OrderEntity entity = mustFind(userId, orderId);
        entity.setStartTime(LocalDateTime.parse(startTime));
        entity.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(entity);
        addOperation(orderId, userId, "RESCHEDULE", startTime);
        return list(userId);
    }

    @Transactional
    public List<Map<String, Object>> refund(Long userId, Long orderId, String reason) {
        violationService.checkUserStatus(userId);
        
        OrderEntity entity = mustFind(userId, orderId);
        
        // 自动监测：恶意退款行为
        if (entity.getStatus() == OrderStatus.COMPLETED || entity.getStatus() == OrderStatus.COMPLETION_PENDING) {
             // 服务已经完成或待审核完成，但仍然退款
             violationService.recordViolation(userId, "MALICIOUS_REFUND", "系统检测到在订单即将完成或已完成时申请退款，疑似恶意退款白嫖服务行为。", String.valueOf(orderId));
        }
        
        entity.setStatus(OrderStatus.REFUND_REQUESTED);
        entity.setRefundReason(reason);
        entity.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(entity);
        addOperation(orderId, userId, "REFUND", reason);
        return list(userId);
    }

    private OrderEntity mustFind(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
    }

    private void addOperation(Long orderId, Long userId, String type, String payload) {
        OrderOperationEntity operation = new OrderOperationEntity();
        operation.setOrderId(orderId);
        operation.setUserId(userId);
        operation.setOperationType(type);
        operation.setPayload(payload);
        operation.setCreatedAt(LocalDateTime.now());
        operationRepository.save(operation);
    }

    /**
     * 与前端 {@code normalizePlayerProfileId} 一致：{@code p2} → {@code "2"}，便于与 players 主键、打手 user.player_profile_id 对齐。
     */
    static String normalizePlayerIdForStorage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.matches("(?i)^p\\d+$")) {
            t = t.substring(1);
        }
        try {
            Long.parseLong(t);
            return t;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 顾客指定打手下单后，给绑定了该档案的打手账号推送站内通知 */
    private void notifyAssignedPlayerUsers(OrderEntity order) {
        String pid = order.getPlayerId();
        if (pid == null || pid.isBlank()) {
            return;
        }
        long profileId;
        try {
            profileId = Long.parseLong(pid.trim());
        } catch (NumberFormatException e) {
            return;
        }
        List<UserEntity> targets = userRepository.findByPlayerProfileId(profileId);
        if (targets.isEmpty()) {
            return;
        }
        String title = "新订单待接单";
        String content = String.format(
                "有顾客指定您接单：%s · %s，金额 ¥%s，订单号 #%s。请尽快在「打手工作台」处理。",
                order.getGame(),
                order.getServiceType(),
                order.getAmount() == null ? "—" : order.getAmount().toPlainString(),
                order.getId());
        String safeContent = content.length() <= 500 ? content : content.substring(0, 500);
        String relatedId = String.valueOf(order.getId());
        for (UserEntity u : targets) {
            NotificationEntity n = new NotificationEntity();
            n.setUserId(u.getId());
            n.setTitle(title);
            n.setContent(safeContent);
            n.setType("order");
            n.setRelatedId(relatedId);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    private Map<String, Object> toOrderDto(OrderEntity o) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", String.valueOf(o.getId()));
        dto.put("game", o.getGame());
        dto.put("gameKey", o.getGameKey());
        dto.put("gameImage", o.getGameImage());
        dto.put("serviceType", o.getServiceType());
        dto.put("status", o.getStatus().name());
        dto.put("statusText", statusText(o.getStatus()));
        dto.put("amount", o.getAmount());
        dto.put("createdAt", o.getCreatedAt().toString());
        dto.put("startTime", o.getStartTime() == null ? null : o.getStartTime().toString());
        dto.put("refundReason", o.getRefundReason());
        dto.put("completionRequestNote", o.getCompletionRequestNote());
        Map<String, Object> player = new HashMap<>();
        player.put("id", o.getPlayerId());
        player.put("name", o.getPlayerName());
        dto.put("player", player);
        return dto;
    }

    private String statusText(OrderStatus status) {
        return switch (status) {
            case PENDING -> "待处理";
            case IN_PROGRESS -> "进行中";
            case COMPLETION_PENDING -> "待审核完成";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case REFUND_REQUESTED -> "退款申请中";
            case REFUNDED -> "已退款";
        };
    }

    @Data
    public static class CreateOrderRequest {
        private String game;
        private String gameKey;
        private String gameImage;
        private String serviceType;
        private BigDecimal amount;
        private String playerId;
        private String playerName;
    }
}
