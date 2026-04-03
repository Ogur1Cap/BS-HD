package com.deltaforce.houduan.support;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "support_tickets")
public class SupportTicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String username;
    private String contact;
    private String problemType;
    private String emergencyLevel;
    private String problemDesc;
    private String status;
    private LocalDateTime createdAt;
}
