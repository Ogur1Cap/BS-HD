package com.deltaforce.houduan.notification;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(notificationService.list(principal.userId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        long n = notificationService.countUnread(principal.userId());
        Map<String, Object> data = new HashMap<>();
        data.put("count", n);
        return ApiResponse.ok(data);
    }

    @PostMapping("/mark-read")
    public ApiResponse<List<Map<String, Object>>> markRead(@RequestBody MarkReadRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(notificationService.markRead(principal.userId(), Long.valueOf(request.getNotificationId())));
    }

    @PostMapping("/mark-all-read")
    public ApiResponse<List<Map<String, Object>>> markAllRead() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(notificationService.markAllRead(principal.userId()));
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<List<Map<String, Object>>> delete(@PathVariable Long notificationId) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(notificationService.delete(principal.userId(), notificationId));
    }

    @Data
    public static class MarkReadRequest {
        private String notificationId;
    }
}
