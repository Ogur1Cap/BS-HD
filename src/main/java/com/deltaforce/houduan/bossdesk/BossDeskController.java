package com.deltaforce.houduan.bossdesk;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.playerjoin.PlayerJoinApplicationService;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/boss-desk")
public class BossDeskController {
    private final BossDeskService bossDeskService;
    private final PlayerJoinApplicationService playerJoinApplicationService;

    public BossDeskController(BossDeskService bossDeskService,
                              PlayerJoinApplicationService playerJoinApplicationService) {
        this.bossDeskService = bossDeskService;
        this.playerJoinApplicationService = playerJoinApplicationService;
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(bossDeskService.stats(p.userId()));
    }

    @GetMapping("/orders/completion-pending")
    public ApiResponse<List<Map<String, Object>>> completionPending() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(bossDeskService.listCompletionPending(p.userId()));
    }

    @GetMapping("/orders/manageable")
    public ApiResponse<List<Map<String, Object>>> manageable() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(bossDeskService.listManageableOrders(p.userId()));
    }

    @GetMapping("/players")
    public ApiResponse<List<Map<String, Object>>> players() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(bossDeskService.listPlayersForReassign(p.userId()));
    }

    @PostMapping("/orders/{orderId}/completion/approve")
    public ApiResponse<Void> approveCompletion(@PathVariable Long orderId) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        bossDeskService.approveCompletion(p.userId(), orderId);
        return ApiResponse.ok();
    }

    @PostMapping("/orders/{orderId}/completion/reject")
    public ApiResponse<Void> rejectCompletion(@PathVariable Long orderId, @RequestBody(required = false) RejectBody body) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        String reason = body == null ? null : body.getReason();
        bossDeskService.rejectCompletion(p.userId(), orderId, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/orders/{orderId}/reassign")
    public ApiResponse<Void> reassign(@PathVariable Long orderId, @RequestBody(required = false) ReassignOrderBody body) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        bossDeskService.reassignOrder(p.userId(), orderId, body);
        return ApiResponse.ok();
    }

    @GetMapping("/join-applications/pending")
    public ApiResponse<List<Map<String, Object>>> joinPending() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerJoinApplicationService.listPendingForBoss(p.userId()));
    }

    @PostMapping("/join-applications/{applicationId}/approve")
    public ApiResponse<Void> approveJoin(@PathVariable Long applicationId) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        playerJoinApplicationService.approveForBoss(p.userId(), applicationId);
        return ApiResponse.ok();
    }

    @PostMapping("/join-applications/{applicationId}/reject")
    public ApiResponse<Void> rejectJoin(@PathVariable Long applicationId, @RequestBody(required = false) RejectBody body) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        String reason = body == null ? null : body.getReason();
        playerJoinApplicationService.rejectForBoss(p.userId(), applicationId, reason);
        return ApiResponse.ok();
    }

    @GetMapping("/player-accounts")
    public ApiResponse<List<Map<String, Object>>> playerAccounts() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerJoinApplicationService.listPlayerAccountsForBoss(p.userId()));
    }

    @PostMapping("/player-accounts/{userId}/revoke")
    public ApiResponse<Void> revokePlayer(@PathVariable Long userId) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        playerJoinApplicationService.revokePlayerForBoss(p.userId(), userId);
        return ApiResponse.ok();
    }

    @Data
    public static class RejectBody {
        private String reason;
    }
}
