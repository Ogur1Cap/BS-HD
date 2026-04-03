package com.deltaforce.houduan.playerjoin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "player_join_applications")
public class PlayerJoinApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private JoinApplicationStatus status;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 500)
    private String intro;

    @Column(length = 255)
    private String skills;

    @Column(name = "rank_name", length = 50)
    private String rankName;

    @Column(length = 255)
    private String tags;

    @Column(name = "price_per_hour", precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "contact_note", length = 255)
    private String contactNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
}
