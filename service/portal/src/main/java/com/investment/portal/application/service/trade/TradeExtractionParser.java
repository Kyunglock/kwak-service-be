package com.investment.portal.application.service.trade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * LLM 응답(JSON)을 {@link ExtractedTrade} 목록으로 바꾼다.
 *
 * <p>LLM 출력은 형식이 어긋나는 것을 전제로 다룬다. 한 줄이 깨졌다고 전체를 버리지 않고
 * 그 줄만 건너뛴다 — 스크린샷 10건 중 1건이 흐릿했다고 나머지 9건을 날릴 이유가 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeExtractionParser {

    private static final int MAX_TRADES = 50;

    /** 숫자만 남기기 위해 제거할 문자 (쉼표, 통화기호, 공백, 단위) */
    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9.\\-]");

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
    };

    private final ObjectMapper objectMapper;

    public List<ExtractedTrade> parse(String llmContent) {
        String json = stripFence(llmContent);
        if (json == null || json.isBlank()) {
            return List.of();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("[TradeCapture] LLM 응답 JSON 파싱 실패: {}", abbreviate(json), e);
            return List.of();
        }

        JsonNode trades = root.path("trades");
        if (!trades.isArray()) {
            log.warn("[TradeCapture] trades 배열이 없음: {}", abbreviate(json));
            return List.of();
        }

        List<ExtractedTrade> result = new ArrayList<>();
        for (JsonNode node : trades) {
            if (result.size() >= MAX_TRADES) {
                log.warn("[TradeCapture] 추출 건수 상한({}) 초과 — 이후 항목 무시", MAX_TRADES);
                break;
            }
            ExtractedTrade trade = toTrade(node);
            if (trade != null) {
                result.add(trade);
            }
        }
        return result;
    }

    private ExtractedTrade toTrade(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String name = text(node, "name");
        String ticker = text(node, "ticker");
        if (name == null && ticker == null) {
            return null; // 종목을 식별할 단서가 전혀 없는 줄은 버린다
        }

        BigDecimal qty = number(node, "qty");
        BigDecimal price = number(node, "price");
        BigDecimal amount = number(node, "amount");

        // 단가를 못 읽었지만 총액과 수량이 있으면 역산한다.
        // 사용자가 화면에서 확인·수정할 것이므로 추정값을 넣어주는 편이 낫다.
        if (price == null && amount != null && qty != null && qty.signum() > 0) {
            price = amount.divide(qty, 4, java.math.RoundingMode.HALF_UP);
        }

        return new ExtractedTrade(
                name,
                ticker == null ? null : ticker.toUpperCase(),
                normalizeType(text(node, "type")),
                qty,
                price,
                amount,
                date(node),
                normalizeCurrency(text(node, "currency"))
        );
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

    private BigDecimal number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        String raw = text(node, field);
        if (raw == null) {
            return null;
        }
        String cleaned = NON_NUMERIC.matcher(raw).replaceAll("");
        if (cleaned.isEmpty() || "-".equals(cleaned) || ".".equals(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.debug("[TradeCapture] 숫자 변환 실패 - field={}, raw={}", field, raw);
            return null;
        }
    }

    private LocalDate date(JsonNode node) {
        String raw = text(node, "date");
        if (raw == null) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, format);
            } catch (Exception ignored) {
                // 다음 형식으로
            }
        }
        log.debug("[TradeCapture] 날짜 변환 실패 - raw={}", raw);
        return null;
    }

    private String normalizeType(String raw) {
        if (raw == null) {
            return "BUY";
        }
        String upper = raw.toUpperCase();
        if (upper.contains("SELL") || raw.contains("매도")) {
            return "SELL";
        }
        return "BUY";
    }

    private String normalizeCurrency(String raw) {
        if (raw == null) {
            return null;
        }
        String upper = raw.toUpperCase();
        if (upper.contains("KRW") || upper.contains("WON") || raw.contains("원")) {
            return "KRW";
        }
        if (upper.contains("USD") || upper.contains("DOLLAR") || raw.contains("달러")) {
            return "USD";
        }
        return upper.length() == 3 ? upper : null;
    }

    private String abbreviate(String s) {
        return s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }
}
