package com.deltaforce.houduan.violation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "violations")
public class ViolationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String type; // FAKE_ORDER, MALICIOUS_REFUND, ILLEGAL_ACCEPT, IMPROPER_SERVICE

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "related_id", length = 50)
    private String relatedId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPEALED, RESOLVED

    @Column(name = "appeal_reason", columnDefinition = "TEXT")
    private String appealReason;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_action", length = 50)
    private String adminAction; // WARNING, RESTRICT, BAN, DISMISS

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}