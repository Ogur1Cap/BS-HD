package com.deltaforce.houduan.auth;

import com.deltaforce.houduan.auth.dto.AuthDtos;
import com.deltaforce.houduan.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        authService.register(request.getUsername(), request.getEmail(), request.getPassword());
        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        return ApiResponse.ok(data);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.getUsername(), request.getPassword());
        Map<String, Object> user = new HashMap<>();
        user.put("username", result.getUsername());
        user.put("email", result.getEmail());
        user.put("phone", result.getPhone());
        user.put("avatar", result.getAvatar());
        user.put("userLevel", result.getUserLevel());

        Map<String, Object> data = new HashMap<>();
        data.put("token", result.getAccessToken());
        data.put("refreshToken", result.getRefreshToken());
        data.put("user", user);
        return ApiResponse.ok(data);
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        AuthService.TokenResult result = authService.refresh(request.getRefreshToken());
        Map<String, Object> data = new HashMap<>();
        data.put("token", result.accessToken());
        return ApiResponse.ok(data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.ok();
    }
}
