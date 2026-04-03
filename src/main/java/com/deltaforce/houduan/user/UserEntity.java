package com.deltaforce.houduan.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 0=顾客 1=打手 2=BOSS（最高权限：审核完成、转派订单） */
    @Column(name = "user_level", nullable = false)
    private int userLevel = 0;

    /** 打手账号关联 players.id，与订单 player_id 一致 */
    @Column(name = "player_profile_id")
    private Long playerProfileId;
}
