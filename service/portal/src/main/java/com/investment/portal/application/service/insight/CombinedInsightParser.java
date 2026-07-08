package com.investment.portal.application.service.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwak.common.ai.JsonExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CombinedInsightParser {

    private final ObjectMapper objectMapper;

    public CombinedInsightParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CombinedInsight parse(String rawContent) {
        String json = JsonExtractor.extractJsonObject(rawContent);
        if (json == null) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode pf = root.get("profile_fit");
            String profileFitJson = (pf != null && pf.isObject())
                    ? objectMapper.writeValueAsString(pf) : null;
            JsonNode dv = root.get("dividend_insight");
            String dividendJson = (dv != null && dv.isObject())
                    ? objectMapper.writeValueAsString(dv) : null;
            return new CombinedInsight(
                    profileFitJson,
                    toLines(root.get("risk_assessment")),
                    toLines(root.get("portfolio_alignment")),
                    toLines(root.get("investment_recommendation")),
                    dividendJson
            );
        } catch (Exception e) {
            log.warn("[Insight] 통합 JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<String> toLines(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) arr.forEach(n -> out.add(n.asText()));
        return out;
    }
}
