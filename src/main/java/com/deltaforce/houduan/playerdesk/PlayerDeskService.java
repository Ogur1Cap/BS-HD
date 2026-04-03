package com.deltaforce.houduan.playerdesk;

import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.notification.NotificationEntity;
import com.deltaforce.houduan.notification.NotificationRepository;
import com.deltaforce.houduan.order.*;
import com.deltaforce.houduan.user.UserEntity;
import com.deltaforce.houduan.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.List.of;

@Service
public class PlayerDeskService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderOperationRepository operationRepository;
    private final NotificationRepository notificationRepository;

    public PlayerDeskService(UserRepository userRepository,
                             OrderRepository orderRepository,
                             OrderOperationRepository operationRepository,
                             NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.operationRepository = operationRepository;
        this.notificationRepository = notificationRepository;
    }

    public Map<String, Object> stats(Long userId) {
        UserEntity user = loadPlayerUser(userId);
        String pid = playerIdString(user);
        Map<String, Object> data = new HashMap<>();
        data.put("pendingCount", orderRepository.findByPlayerIdAndStatusOrderByCreatedAtDesc(pid, OrderStatus.PENDING).size());
        data.put("servingCount", orderRepository
                .findByPlayerIdAndStatusInOrderByCreatedAtDesc(pid, of(OrderStatus.IN_PROGRESS, OrderStatus.COMPLETION_PENDING))
                .size());
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long doneWeek = orderRepository.findByPlayerIdOrderByCreatedAtDesc(pid).stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED
                        && o.getUpdatedAt() != null
                        && !o.getUpdatedAt().isBefore(weekAgo))
                .count();
        data.put("completedWeekCount", doneWeek);
        data.put("playerProfileId", pid);
        data.put("displayHint", "接单后请及时与顾客确认上号时间；拒单将通知顾客重新匹配。");
        return data;
    }

    public List<Map<String, Object>> listPending(Long userId) {
        UserEntity user = loadPlayerUser(userId);
        String pid = playerIdString(user);
        return orderRepository.findByPlayerIdAndStatusOrderByCreatedAtDesc(pid, OrderStatus.PENDING).stream()
                .map(this::toDeskDto)
                .toList();
    }

    public List<Map<String, Object>> listServing(Long userId) {
        UserEntity user = loadPlayerUser(userId);
        String pid = playerIdString(user);
        return orderRepository
                .findByPlayerIdAndStatusInOrderByCreatedAtDesc(pid, of(OrderStatus.IN_PROGRESS, OrderStatus.COMPLETION_PENDING))
                .stream()
                .map(this::toDeskDto)
                .toList();
    }

    /**
     * 打手申请订单完成，进入待 BOSS 审核状态。
     */
    @Transactional
    public List<Map<String, Object>> requestOrderCompletion(Long userId, Long orderId, String note) {
        UserEntity user = loadPlayerUser(userId);
        String pid = playerIdString(user);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!pid.equals(order.getPlayerId())) {
            throw new BizException(403, "无权操作此订单");
        }
        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new BizException(400, "仅进行中的订单可申请完成");
        }
        String n = note == null ? "" : note.trim();
        order.setStatus(OrderStatus.COMPLETION_PENDING);
        order.setCompletionRequestNote(n.isEmpty() ? null : n);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        logOp(orderId, userId, "PLAYER_REQUEST_COMPLETE", n.isEmpty() ? "requested" : n);
        notifyCustomer(order.getUserId(), "订单完成待平台确认",
                String.format("打手已提交订单 #%s（%s）的完成申请，平台审核通过后将标记为完成。", orderId, order.getServiceType()),
                String.valueOf(orderId));
        notifyBosses("待审核：打手申请完成",
                String.format("订单 #%s，打手 %s 申请完成。说明：%s",
                        orderId, user.getUsername(), n.isEmpty() ? "（无）" : n),
                String.valueOf(orderId));
        return listServing(userId);
    }

    @Transactional
    public List<Map<String, Object>> acceptOrder(Long userId, Long orderId) {
        UserEntity user = loadPlayerUser(userId);
        String pid = playerIdString(user);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!pid.equals(order.getPlayerId())) {
            throw new BizException(403, "无权操作此订单");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BizException(400, "订单状态不可接单");
        }
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setUpdatedAt(LocalDateTime.now());
        if (order.getStartTime() == null) {
            order.setStartTime(LocalDateTime.now().plusHours(1));
        }
        orderRepository.save(order);
        logOp(orderId, userId, "PLAYER_ACCEPT", "accepted");
        notifyCustomer(order.getUserId(), "打手已接单",
                String.format("您指定的打手已接单，订单 #%s（%s）将进入服务流程，请按约定时间上号。", orderId, order.getServiceType()),
                String.valueOf(orderId));
        return listPending(userId);
    }

    @Transactional
    public List<Map<String, Object>> rejectOrder(Long userId, Long orderId, String reason) {
        UserEntity user = loadPlayerUser(userId);
        String pid = playerIdString(user);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!pid.equals(order.getPlayerId())) {
            throw new BizException(403, "无权操作此订单");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BizException(400, "订单状态不可拒单");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        String payload = reason == null || reason.isBlank() ? "rejected" : reason.trim();
        logOp(orderId, userId, "PLAYER_REJECT", payload);
        String tip = reason == null || reason.isBlank()
                ? "打手暂时无法承接本单，订单已取消，您可重新下单或选择其他打手。"
                : ("打手拒单说明：" + reason.trim() + "。订单已取消，您可重新匹配。");
        notifyCustomer(order.getUserId(), "打手无法接单", tip, String.valueOf(orderId));
        return listPending(userId);
    }

    private UserEntity loadPlayerUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        if (user.getUserLevel() != 1) {
            throw new BizException(403, "需要打手账号（1 级）才能使用工作台");
        }
        if (user.getPlayerProfileId() == null) {
            throw new BizException(400, "账号未绑定打手档案，请联系管理员");
        }
        return user;
    }

    private static String playerIdString(UserEntity user) {
        return String.valueOf(user.getPlayerProfileId());
    }

    private Map<String, Object> toDeskDto(OrderEntity o) {
        String customerName = userRepository.findById(o.getUserId())
                .map(UserEntity::getUsername)
                .orElse("顾客");
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", String.valueOf(o.getId()));
        dto.put("game", o.getGame());
        dto.put("gameKey", o.getGameKey());
        dto.put("gameImage", o.getGameImage());
        dto.put("serviceType", o.getServiceType());
        dto.put("status", o.getStatus().name());
        dto.put("statusText", statusText(o.getStatus()));
        dto.put("completionRequestNote", o.getCompletionRequestNote());
        dto.put("amount", o.getAmount());
        dto.put("createdAt", o.getCreatedAt().toString());
        dto.put("startTime", o.getStartTime() == null ? null : o.getStartTime().toString());
        dto.put("customerUsername", customerName);
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

    /** 通知所有 BOSS 账号（user_level=2） */
    private void notifyBosses(String title, String content, String relatedId) {
        List<UserEntity> bosses = userRepository.findByUserLevel(2);
        for (UserEntity b : bosses) {
            NotificationEntity n = new NotificationEntity();
            n.setUserId(b.getId());
            n.setTitle(title);
            n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
            n.setType("boss");
            n.setRelatedId(relatedId);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }
}
