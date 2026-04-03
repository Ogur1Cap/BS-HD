package com.deltaforce.houduan.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(length = 255)
    private String avatar;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String bio;

    @Column(length = 100)
    private String gamePreference;
}
