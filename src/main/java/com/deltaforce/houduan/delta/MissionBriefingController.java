package com.deltaforce.houduan.delta;

import com.deltaforce.houduan.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/delta")
public class MissionBriefingController {
    private final MissionBriefingService missionBriefingService;

    public MissionBriefingController(MissionBriefingService missionBriefingService) {
        this.missionBriefingService = missionBriefingService;
    }

    @GetMapping("/mission-briefing")
    public ApiResponse<Map<String, Object>> missionBriefing() {
        return ApiResponse.ok(missionBriefingService.briefingFor(LocalDate.now()));
    }
}
