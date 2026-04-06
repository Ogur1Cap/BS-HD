package com.deltaforce.houduan.violation;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "违规风控管理", description = "风控预警、申诉与处理接口")
@RestController
@RequestMapping("/api/violation")
public class ViolationController {

    private final ViolationService violationService;

    public ViolationController(ViolationService violationService) {
        this.violationService = violationService;
    }

    @Operation(summary = "用户获取个人违规记录")
    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> getMyViolations(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ApiResponse.ok(violationService.listUserViolations(principal.userId()));
    }

    @Operation(summary = "用户提交违规申诉")
    @PostMapping("/appeal/{id}")
    public ApiResponse<String> submitAppeal(@AuthenticationPrincipal JwtUserPrincipal principal,
                                          @PathVariable Long id,
                                          @RequestBody AppealRequest request) {
        violationService.submitAppeal(principal.userId(), id, request.getReason());
        return ApiResponse.ok("ok");
    }

    @Operation(summary = "BOSS获取待处理预警")
    @GetMapping("/boss/pending")
    public ApiResponse<List<Map<String, Object>>> getPendingViolations(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireBoss(principal);
        return ApiResponse.ok(violationService.listPendingViolations(principal.userId()));
    }

    @Operation(summary = "BOSS获取申诉列表")
    @GetMapping("/boss/appealed")
    public ApiResponse<List<Map<String, Object>>> getAppealedViolations(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireBoss(principal);
        return ApiResponse.ok(violationService.listAppealedViolations(principal.userId()));
    }

    @Operation(summary = "BOSS获取所有违规记录")
    @GetMapping("/boss/all")
    public ApiResponse<List<Map<String, Object>>> getAllViolations(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireBoss(principal);
        return ApiResponse.ok(violationService.listAllViolations(principal.userId()));
    }

    @Operation(summary = "BOSS处理违规/申诉")
    @PostMapping("/boss/handle/{id}")
    public ApiResponse<String> handleViolation(@AuthenticationPrincipal JwtUserPrincipal principal,
                                             @PathVariable Long id,
                                             @RequestBody HandleViolationRequest request) {
        requireBoss(principal);
        violationService.handleViolation(principal.userId(), id, request.getAction(), request.getNotes());
        return ApiResponse.ok("ok");
    }

    private void requireBoss(JwtUserPrincipal principal) {
        if (principal.userLevel() < 2) {
            throw new RuntimeException("需要BOSS权限");
        }
    }

    @Data
    public static class AppealRequest {
        private String reason;
    }

    @Data
    public static class HandleViolationRequest {
        private String action; // WARNING, RESTRICT, BAN, DISMISS
        private String notes;
    }
}