package com.deltaforce.houduan.player;

import com.deltaforce.houduan.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/player-hall")
public class PlayerHallController {
    private final PlayerRepository playerRepository;

    public PlayerHallController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping("/players")
    public ApiResponse<List<Map<String, Object>>> players() {
        List<Map<String, Object>> data = playerRepository.findByShowInHallIsTrueOrderByIdAsc().stream()
                .map(this::toFrontendRow)
                .toList();
        return ApiResponse.ok(data);
    }

    private Map<String, Object> toFrontendRow(PlayerEntity p) {
        String rankText = (p.getRankName() == null || p.getRankName().isBlank()) ? "铂金" : p.getRankName().trim();
        String rankCode = resolveRankCode(rankText);

        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(p.getId()));
        m.put("name", p.getName());
        m.put("avatar", p.getAvatar() == null ? "" : p.getAvatar());
        // 前端筛选使用 legend/master/diamond/platinum
        m.put("rank", rankCode);
        m.put("rankText", rankText);
        m.put("rankColor", rankColor(rankCode));
        m.put("skills", splitSkills(p.getSkills()));
        m.put("winRate", toIntPercent(p.getWinRate()));
        m.put("completedOrders", p.getCompletedOrders() == null ? 0 : p.getCompletedOrders());
        m.put("rating", p.getRating() == null ? BigDecimal.ZERO : p.getRating());
        m.put("pricePerHour", p.getPricePerHour() == null ? BigDecimal.ZERO : p.getPricePerHour());
        m.put("intro", p.getIntro() == null ? "" : p.getIntro());
        m.put("tags", splitTags(p.getTags()));
        return m;
    }

    /**
     * 兼容库内存中文段位或英文 key。
     */
    private static String resolveRankCode(String rankText) {
        String t = rankText.toLowerCase();
        if (t.equals("legend") || rankText.contains("传奇")) {
            return "legend";
        }
        if (t.equals("master") || rankText.contains("大师")) {
            return "master";
        }
        if (t.equals("diamond") || rankText.contains("钻石")) {
            return "diamond";
        }
        if (t.equals("platinum") || rankText.contains("铂金")) {
            return "platinum";
        }
        return "platinum";
    }

    private static String rankColor(String rankCode) {
        return switch (rankCode) {
            case "legend" -> "#f59e0b";
            case "master" -> "#8b5cf6";
            case "diamond" -> "#3b82f6";
            default -> "#64748b";
        };
    }

    private static List<String> splitSkills(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split("\\s*,\\s*"));
    }

    private static List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split("\\s*,\\s*"));
    }

    private static int toIntPercent(BigDecimal winRate) {
        if (winRate == null) {
            return 0;
        }
        return winRate.intValue();
    }
}
