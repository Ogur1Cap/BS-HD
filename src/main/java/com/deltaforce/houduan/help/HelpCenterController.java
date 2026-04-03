package com.deltaforce.houduan.help;

import com.deltaforce.houduan.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/help-center")
public class HelpCenterController {
    private final FaqItemRepository faqItemRepository;

    public HelpCenterController(FaqItemRepository faqItemRepository) {
        this.faqItemRepository = faqItemRepository;
    }

    @GetMapping("/faqs")
    public ApiResponse<List<Map<String, Object>>> faqs(@RequestParam(required = false) String category) {
        List<FaqItemEntity> list = (category == null || category.isBlank())
                ? faqItemRepository.findAll()
                : faqItemRepository.findByCategory(category);
        List<Map<String, Object>> data = list.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(f.getId()));
            m.put("category", f.getCategory());
            m.put("question", f.getQuestion());
            m.put("answer", f.getAnswer());
            return m;
        }).toList();
        return ApiResponse.ok(data);
    }
}
