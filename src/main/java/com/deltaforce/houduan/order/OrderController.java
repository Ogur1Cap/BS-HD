package com.deltaforce.houduan.order;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(orderService.list(principal.userId()));
    }

    @PostMapping
    public ApiResponse<List<Map<String, Object>>> create(@RequestBody OrderService.CreateOrderRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(orderService.create(principal.userId(), request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long orderId) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(orderService.detail(principal.userId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<List<Map<String, Object>>> cancel(@PathVariable Long orderId) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(orderService.cancel(principal.userId(), orderId));
    }

    @PostMapping("/{orderId}/reschedule")
    public ApiResponse<List<Map<String, Object>>> reschedule(@PathVariable Long orderId, @RequestBody RescheduleRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(orderService.reschedule(principal.userId(), orderId, request.getStartTime()));
    }

    @PostMapping("/{orderId}/refund")
    public ApiResponse<List<Map<String, Object>>> refund(@PathVariable Long orderId, @RequestBody RefundRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(orderService.refund(principal.userId(), orderId, request.getReason()));
    }

    @Data
    public static class RescheduleRequest {
        @NotBlank(message = "startTime 不能为空")
        private String startTime;
    }

    @Data
    public static class RefundRequest {
        private String reason;
    }
}
