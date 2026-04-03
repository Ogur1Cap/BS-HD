package com.deltaforce.houduan.playerjoin;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/player-join-applications")
public class PlayerJoinController {
    private final PlayerJoinApplicationService playerJoinApplicationService;

    public PlayerJoinController(PlayerJoinApplicationService playerJoinApplicationService) {
        this.playerJoinApplicationService = playerJoinApplicationService;
    }

    /** 顾客提交「加入我们」成为打手的申请 */
    @PostMapping
    public ApiResponse<Map<String, Object>> submit(@RequestBody PlayerJoinApplicationService.SubmitJoinBody body) {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerJoinApplicationService.submit(p.userId(), body));
    }

    /** 当前用户最近一次申请状态 */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        JwtUserPrincipal p = SecurityUtils.currentUser();
        return ApiResponse.ok(playerJoinApplicationService.getMy(p.userId()));
    }
}
