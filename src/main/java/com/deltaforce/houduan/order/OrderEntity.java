package com.deltaforce.houduan.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String game;

    @Column(nullable = false, length = 50)
    private String gameKey;

    @Column(length = 255)
    private String gameImage;

    @Column(nullable = false, length = 50)
    private String serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String playerId;

    @Column(length = 100)
    private String playerName;

    private LocalDateTime startTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(length = 255)
    private String refundReason;

    /** 打手申请完成时填写的说明（仅 COMPLETION_PENDING 时有意义） */
    @Column(name = "completion_request_note", length = 500)
    private String completionRequestNote;
}
