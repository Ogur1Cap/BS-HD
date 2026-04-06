package com.deltaforce.houduan.settings;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import com.deltaforce.houduan.user.UserProfileRepository;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/account-settings")
public class AccountSettingsController {
    private final UserSettingsRepository repository;
    private final UserProfileRepository profileRepository;

    public AccountSettingsController(UserSettingsRepository repository, UserProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> get() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserSettingsEntity s = repository.findById(principal.userId()).orElseGet(() -> {
            UserSettingsEntity created = new UserSettingsEntity();
            created.setUserId(principal.userId());
            created.setNotifyChannels("app,email");
            created.setNotifyTypes("order,system,message");
            created.setUpdatedAt(LocalDateTime.now());
            return repository.save(created);
        });
        return ApiResponse.ok(toMap(s));
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> update(@RequestBody UpdateSettingsRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserSettingsEntity s = repository.findById(principal.userId()).orElseGet(() -> {
            UserSettingsEntity created = new UserSettingsEntity();
            created.setUserId(principal.userId());
            created.setNotifyChannels("app,email");
            created.setNotifyTypes("order,system,message");
            return created;
        });
        s.setUserId(principal.userId());
        if (request.getNickname() != null) {
            s.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            s.setBio(request.getBio());
        }
        if (request.getNotifyChannels() != null) {
            s.setNotifyChannels(request.getNotifyChannels());
        }
        if (request.getNotifyTypes() != null) {
            s.setNotifyTypes(request.getNotifyTypes());
        }
        if (request.getWechat() != null) {
            s.setWechat(request.getWechat());
        }
        if (request.getQq() != null) {
            s.setQq(request.getQq());
        }
        if (request.getWeibo() != null) {
            s.setWeibo(request.getWeibo());
        }
        s.setUpdatedAt(LocalDateTime.now());
        repository.save(s);
        // 个人简介与 /profile 共用 user_profiles
        if (request.getBio() != null) {
            profileRepository.findById(principal.userId()).ifPresent(p -> {
                p.setBio(request.getBio());
                profileRepository.save(p);
            });
        }
        return ApiResponse.ok(toMap(s));
    }

    private Map<String, Object> toMap(UserSettingsEntity s) {
        Map<String, Object> m = new HashMap<>();
        m.put("nickname", s.getNickname());
        m.put("bio", s.getBio());
        m.put("notifyChannels", s.getNotifyChannels());
        m.put("notifyTypes", s.getNotifyTypes());
        m.put("wechat", s.getWechat());
        m.put("qq", s.getQq());
        m.put("weibo", s.getWeibo());
        return m;
    }

    @Data
    public static class UpdateSettingsRequest {
        private String nickname;
        private String bio;
        private String notifyChannels;
        private String notifyTypes;
        private String wechat;
        private String qq;
        private String weibo;
    }
}
