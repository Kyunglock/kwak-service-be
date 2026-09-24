package com.investment.portal.application.service.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM 응답(JSON)을 {@link ExtractedQuestionIntent}로 바꾼다.
 *
 * <p>TradeExtractionParser 와 같은 전제 — LLM 출력은 형식이 어긋나는 것을 전제로
 * 다룬다. 파싱에 실패하면 예외를 던지지 않고 {@link QuestionType#UNKNOWN}으로
 * 흡수한다(호출부가 "질문을 이해하지 못했다"로 안전하게 답할 수 있게).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionIntentParser {

    private final ObjectMapper objectMapper;

    public ExtractedQuestionIntent parse(String llmContent) {
        String json = stripFence(llmContent);
        if (json == null || json.isBlank()) {
            return unknown();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("[MarketQuestion] LLM 응답 JSON 파싱 실패: {}", abbreviate(json), e);
            return unknown();
        }

        // questionType 화이트리스트 검증은 QuestionType.fromRaw 가 한다 —
        // 여기서는 원문 문자열만 그대로 옮긴다.
        return new ExtractedQuestionIntent(
                text(root, "questionType"),
                text(root, "stockName"),
                text(root, "periodHint"));
    }

    private ExtractedQuestionIntent unknown() {
        return new ExtractedQuestionIntent(QuestionType.UNKNOWN.name(), null, null);
    }

    /** 모델이 마크다운 코드블록으로 감싸는 경우 대비 — 첫 '{' 부터 마지막 '}' 까지만 취한다. */
    private String stripFence(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            return null;
        }
        return trimmed.substring(start, end + 1);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String s = value.asText().trim();
        // LLM이 null을 문자열로 내보내는 경우가 있다
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || "N/A".equalsIgnoreCase(s) || "-".equals(s)) {
            return null;
        }
        return s;
    }

    private String abbreviate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
