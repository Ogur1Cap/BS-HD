package com.deltaforce.houduan.auth;

import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.config.JwtProperties;
import com.deltaforce.houduan.security.JwtTokenProvider;
import com.deltaforce.houduan.user.UserEntity;
import com.deltaforce.houduan.user.UserProfileEntity;
import com.deltaforce.houduan.user.UserProfileRepository;
import com.deltaforce.houduan.user.UserRepository;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public void register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BizException(4001, "用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BizException(4002, "邮箱已存在");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUserLevel(0);
        user.setPlayerProfileId(null);
        userRepository.save(user);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setUser(user);
        profile.setAvatar("");
        profile.setPhone("");
        profile.setBio("");
        profile.setGamePreference("");
        userProfileRepository.save(profile);
    }

    @Transactional
    public LoginResult login(String username, String password) {
        String loginKey = username == null ? "" : username.trim();
        UserEntity user = userRepository.findByUsername(loginKey)
                .or(() -> userRepository.findByEmail(loginKey))
                .orElseThrow(() -> new BizException(4003, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(4003, "用户名或密码错误");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getUserLevel());
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(user.getId());
        entity.setToken(refreshToken);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpireDays()));
        refreshTokenRepository.save(entity);

        UserProfileEntity profile = userProfileRepository.findById(user.getId()).orElse(null);
        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(profile != null ? profile.getPhone() : "")
                .avatar(profile != null ? profile.getAvatar() : "")
                .userLevel(user.getUserLevel())
                .build();
    }

    @Transactional
    public TokenResult refresh(String refreshToken) {
        RefreshTokenEntity entity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BizException(4011, "refresh token 无效"));
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(entity);
            throw new BizException(4012, "refresh token 已过期");
        }
        UserEntity user = userRepository.findById(entity.getUserId())
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getUserLevel());
        return new TokenResult(newAccessToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    /**
     * 修改登录密码（需校验当前密码）
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BizException(4003, "当前密码错误");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 20) {
            throw new BizException(400, "新密码长度需在 8-20 位之间");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Getter
    @Builder
    public static class LoginResult {
        private String accessToken;
        private String refreshToken;
        private String username;
        private String email;
        private String phone;
        private String avatar;
        private int userLevel;
    }

    public record TokenResult(String accessToken) {
    }
}
