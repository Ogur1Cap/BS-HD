package com.deltaforce.houduan.support;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/support-center")
public class SupportCenterController {
    private final SupportTicketRepository supportTicketRepository;

    public SupportCenterController(SupportTicketRepository supportTicketRepository) {
        this.supportTicketRepository = supportTicketRepository;
    }

    @PostMapping("/tickets")
    public ApiResponse<Map<String, Object>> createTicket(@RequestBody CreateTicketRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        SupportTicketEntity t = new SupportTicketEntity();
        t.setUserId(principal.userId());
        t.setUsername(request.getUsername());
        t.setContact(request.getContact());
        t.setProblemType(request.getProblemType());
        t.setEmergencyLevel(request.getEmergencyLevel());
        t.setProblemDesc(request.getProblemDesc());
        t.setStatus("QUEUED");
        t.setCreatedAt(LocalDateTime.now());
        supportTicketRepository.save(t);
        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(t.getId()));
        data.put("status", t.getStatus());
        return ApiResponse.ok(data);
    }

    @GetMapping("/tickets")
    public ApiResponse<List<Map<String, Object>>> myTickets() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        List<Map<String, Object>> data = supportTicketRepository.findByUserIdOrderByCreatedAtDesc(principal.userId()).stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(t.getId()));
            m.put("problemType", t.getProblemType());
            m.put("emergencyLevel", t.getEmergencyLevel());
            m.put("status", t.getStatus());
            m.put("createdAt", t.getCreatedAt().toString());
            return m;
        }).toList();
        return ApiResponse.ok(data);
    }

    @Data
    public static class CreateTicketRequest {
        private String username;
        private String contact;
        private String problemType;
        private String emergencyLevel;
        private String problemDesc;
    }
}
