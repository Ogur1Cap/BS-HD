package com.deltaforce.houduan.user;

import com.deltaforce.houduan.auth.AuthService;
import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.config.UploadProperties;
import com.deltaforce.houduan.settings.UserSettingsRepository;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private static final List<String> ALLOWED_EXT = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UploadProperties uploadProperties;
    private final AuthService authService;

    public ProfileController(UserRepository userRepository,
                             UserProfileRepository profileRepository,
                             UserSettingsRepository userSettingsRepository,
                             UploadProperties uploadProperties,
                             AuthService authService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.uploadProperties = uploadProperties;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getProfile() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        UserProfileEntity profile = profileRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(404, "用户资料不存在"));

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("phone", profile.getPhone() == null ? "" : profile.getPhone());
        data.put("avatar", profile.getAvatar() == null ? "" : profile.getAvatar());
        data.put("bio", profile.getBio() == null ? "" : profile.getBio());
        data.put("gamePreference", profile.getGamePreference() == null ? "" : profile.getGamePreference());
        data.put("userLevel", user.getUserLevel());
        data.put("playerProfileId", user.getPlayerProfileId() == null ? null : String.valueOf(user.getPlayerProfileId()));
        return ApiResponse.ok(data);
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        UserProfileEntity profile = profileRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(404, "用户资料不存在"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String nu = request.getUsername().trim();
            if (!nu.matches("^[a-zA-Z0-9_]{3,20}$")) {
                throw new BizException(400, "用户名需为 3-20 位字母、数字或下划线");
            }
            if (!nu.equals(user.getUsername()) && userRepository.existsByUsername(nu)) {
                throw new BizException(4001, "用户名已存在");
            }
            user.setUsername(nu);
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String em = request.getEmail().trim();
            userRepository.findByEmail(em)
                    .filter(u -> !u.getId().equals(user.getId()))
                    .ifPresent(u -> {
                        throw new BizException(4002, "邮箱已被使用");
                    });
            user.setEmail(em);
        }

        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            profile.setAvatar(request.getAvatar());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getGamePreference() != null) {
            profile.setGamePreference(request.getGamePreference());
        }

        userRepository.save(user);
        profileRepository.save(profile);
        // 若已存在账户设置行，同步简介，与 /account-settings 展示一致
        if (request.getBio() != null) {
            userSettingsRepository.findById(principal.userId()).ifPresent(settings -> {
                settings.setBio(profile.getBio());
                settings.setUpdatedAt(LocalDateTime.now());
                userSettingsRepository.save(settings);
            });
        }
        return getProfile();
    }

    /**
     * 上传头像文件，返回可访问的相对路径（需配合前端拼接 API 根地址）。
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(400, "仅支持上传图片");
        }

        String ext = resolveExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path base = Path.of(uploadProperties.dirOrDefault()).toAbsolutePath().normalize();
        Path avatarsDir = base.resolve("avatars");
        Files.createDirectories(avatarsDir);
        Path target = avatarsDir.resolve(filename);
        file.transferTo(target.toFile());

        String url = "/uploads/avatars/" + filename;
        return ApiResponse.ok(Map.of("url", url));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        authService.changePassword(principal.userId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.ok();
    }

    private static String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            return ".jpg";
        }
        return ext;
    }

    @Data
    public static class UpdateProfileRequest {
        private String username;
        @Email(message = "邮箱格式不正确")
        private String email;
        private String phone;
        private String avatar;
        private String bio;
        private String gamePreference;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "请输入当前密码")
        private String currentPassword;
        @NotBlank(message = "请输入新密码")
        private String newPassword;
    }
}
