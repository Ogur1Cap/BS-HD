package com.deltaforce.houduan.map;

import com.deltaforce.houduan.common.ApiResponse;
import com.deltaforce.houduan.common.BizException;
import com.deltaforce.houduan.security.JwtUserPrincipal;
import com.deltaforce.houduan.security.SecurityUtils;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/game-map")
public class GameMapController {
    private final MapPoiRepository poiRepository;
    private final UserMapMarkerRepository markerRepository;

    public GameMapController(MapPoiRepository poiRepository, UserMapMarkerRepository markerRepository) {
        this.poiRepository = poiRepository;
        this.markerRepository = markerRepository;
    }

    @GetMapping("/pois")
    public ApiResponse<List<Map<String, Object>>> listPois() {
        List<Map<String, Object>> data = poiRepository.findAll().stream().map(this::poiToMap).toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/markers")
    public ApiResponse<List<Map<String, Object>>> listMarkers() {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        return ApiResponse.ok(markersPayload(principal.userId()));
    }

    @PostMapping("/markers")
    public ApiResponse<List<Map<String, Object>>> addMarker(@RequestBody CreateMarkerRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserMapMarkerEntity entity = new UserMapMarkerEntity();
        entity.setUserId(principal.userId());
        entity.setX(MapCsvUtil.roundCoord(request.getX()));
        entity.setY(MapCsvUtil.roundCoord(request.getY()));
        entity.setLabel(request.getLabel() == null || request.getLabel().isBlank() ? "未命名标记" : request.getLabel().trim());
        entity.setNote(request.getNote() == null ? "" : request.getNote());
        entity.setCreatedAt(LocalDateTime.now());
        markerRepository.save(entity);
        return ApiResponse.ok(markersPayload(principal.userId()));
    }

    @PutMapping("/markers/{markerId}")
    public ApiResponse<List<Map<String, Object>>> updateMarker(
            @PathVariable Long markerId,
            @RequestBody UpdateMarkerRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserMapMarkerEntity entity = markerRepository.findByIdAndUserId(markerId, principal.userId())
                .orElseThrow(() -> new BizException(404, "标记不存在或无权操作"));
        if (request.getLabel() != null) {
            entity.setLabel(request.getLabel().isBlank() ? entity.getLabel() : request.getLabel().trim());
        }
        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }
        if (request.getX() != null) {
            entity.setX(MapCsvUtil.roundCoord(request.getX()));
        }
        if (request.getY() != null) {
            entity.setY(MapCsvUtil.roundCoord(request.getY()));
        }
        markerRepository.save(entity);
        return ApiResponse.ok(markersPayload(principal.userId()));
    }

    @DeleteMapping("/markers/{markerId}")
    public ApiResponse<List<Map<String, Object>>> deleteMarker(@PathVariable Long markerId) {
        JwtUserPrincipal principal = SecurityUtils.currentUser();
        UserMapMarkerEntity entity = markerRepository.findByIdAndUserId(markerId, principal.userId())
                .orElseThrow(() -> new BizException(404, "标记不存在或无权操作"));
        markerRepository.delete(entity);
        return ApiResponse.ok(markersPayload(principal.userId()));
    }

    private Map<String, Object> poiToMap(MapPoiEntity p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(p.getId()));
        m.put("name", p.getName());
        m.put("x", p.getX());
        m.put("y", p.getY());
        m.put("floor", MapCsvUtil.parseFloor(p.getFloor()));
        m.put("type", p.getType());
        m.put("modes", MapCsvUtil.splitCsv(p.getModes()));
        m.put("security", MapCsvUtil.splitCsv(p.getSecurityLevels()));
        return m;
    }

    private List<Map<String, Object>> markersPayload(Long userId) {
        return markerRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(p.getId()));
            m.put("x", p.getX() == null ? 0.0 : p.getX().doubleValue());
            m.put("y", p.getY() == null ? 0.0 : p.getY().doubleValue());
            m.put("label", p.getLabel());
            m.put("note", p.getNote() == null ? "" : p.getNote());
            return m;
        }).toList();
    }

    @Data
    public static class CreateMarkerRequest {
        private Double x;
        private Double y;
        private String label;
        private String note;
    }

    @Data
    public static class UpdateMarkerRequest {
        private String label;
        private String note;
        private Double x;
        private Double y;
    }
}
