package com.deltaforce.houduan.map;

import java.util.Arrays;
import java.util.List;

/**
 * 地图 POI 在库中以逗号分隔字符串存储，接口层转为 JSON 数组供前端使用。
 */
final class MapCsvUtil {
    private MapCsvUtil() {
    }

    static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static int parseFloor(String floor) {
        if (floor == null || floor.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(floor.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    static int roundCoord(Double v) {
        if (v == null || v.isNaN()) {
            return 0;
        }
        return (int) Math.round(v);
    }
}
