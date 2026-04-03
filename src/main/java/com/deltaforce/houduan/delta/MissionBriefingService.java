package com.deltaforce.houduan.delta;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「今日行动简报」：按日期稳定轮换的战术小贴士，贴合三角洲护航主题。
 */
@Service
public class MissionBriefingService {

    private static final List<String> TIPS = List.of(
            "开局先听枪声方向，避免盲冲开阔地；撤离前留一条备用路线。",
            "机密/绝密局优先控高价值物资点，背包空间留给单格高价物品。",
            "与打手沟通时明确：目标地图、期望时长、是否允许刚枪。",
            "遇到可疑行为请保留录像与时间戳，便于客服快速介入。",
            "夜图记得带夜视或热成像配件，进室内先清角再舔包。",
            "组排分工：一人信息、一人突破、一人架枪，比扎堆冲点更稳。",
            "任务链优先做「顺路」目标，减少折返降低暴露时间。"
    );

    public Map<String, Object> briefingFor(LocalDate date) {
        int idx = Math.floorMod(date.getDayOfYear(), TIPS.size());
        Map<String, Object> data = new HashMap<>();
        data.put("title", "今日行动简报");
        data.put("mapName", "零号大坝");
        data.put("date", date.toString());
        data.put("tip", TIPS.get(idx));
        data.put("tag", "战术提示");
        return data;
    }
}
