package com.deltaforce.houduan.playerdesk;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/player-desk")
public class PlayerDeskController {
    private final PlayerDeskService playerDeskService;

    public PlayerDeskController(PlayerDeskService playerDeskService) {
        this.playerDeskService = playerDeskService;
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerDeskService.stats(p.userId()));
    }

    @GetMapping("/orders/pending")
    public ApiResponse<List<Map<String, Object>>> pending() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerDeskService.listPending(p.userId()));
    }

    @GetMapping("/orders/serving")
    public ApiResponse<List<Map<String, Object>>> serving() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerDeskService.listServing(p.userId()));
    }

    @PostMapping("/orders/{orderId}/accept")
    public ApiResponse<List<Map<String, Object>>> accept(@PathVariable Long orderId) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerDeskService.acceptOrder(p.userId(), orderId));
    }

    @PostMapping("/orders/{orderId}/reject")
    public ApiResponse<List<Map<String, Object>>> reject(@PathVariable Long orderId, @RequestBody(required = false) RejectBody body) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        String reason = body == null ? null : body.getReason();
        return ApiResponse.ok(playerDeskService.rejectOrder(p.userId(), orderId, reason));
    }

    /** 打手提交「完成订单」申请，待 BOSS 审核 */
    @PostMapping("/orders/{orderId}/complete-request")
    public ApiResponse<List<Map<String, Object>>> completeRequest(@PathVariable Long orderId, @RequestBody(required = false) CompleteNoteBody body) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        String note = body == null ? null : body.getNote();
        return ApiResponse.ok(playerDeskService.requestOrderCompletion(p.userId(), orderId, note));
    }

    @Data
    public static class RejectBody {
        private String reason;
    }

    @Data
    public static class CompleteNoteBody {
        private String note;
    }
}
