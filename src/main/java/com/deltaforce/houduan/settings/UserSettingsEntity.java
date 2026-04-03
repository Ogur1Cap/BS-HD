package com.deltaforce.houduan.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {
    @Id
    private Long userId;
    private String nickname;
    private String bio;
    private String notifyChannels;
    private String notifyTypes;
    private String wechat;
    private String qq;
    private String weibo;
    private LocalDateTime updatedAt;
}
