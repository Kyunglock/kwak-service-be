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
 * LLM 응답(JSON)을 {@link TradeExtraction} 으로 바꾼다.
 *
 * <p>LLM 출력은 형식이 어긋나는 것을 전제로 다룬다. 한 줄이 깨졌다고 전체를 버리지 않고
 * 그 줄만 건너뛴다 — 스크린샷 10건 중 1건이 흐릿했다고 나머지 9건을 날릴 이유가 없다.
 *
 * <p>추출이 0건으로 끝나면 원문을 로그에 남긴다. 0건은 "화면에 매매가 없었다"일 수도,
 * "모델이 형식을 어겼다"일 수도 있는데, 원문이 없으면 둘을 영영 구분할 수 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeExtractionParser {

    private static final int MAX_TRADES = 50;

    /** 숫자만 남기기 위해 제거할 문자 (쉼표, 통화기호, 공백, 단위) */
    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9.\\-]");

    /** 추론 흔적을 내보내는 모델 대비 — 그 안의 중괄호에 파서가 끌려가면 안 된다. */
    private static final Pattern THINK_BLOCK =
            Pattern.compile("<think>.*?</think>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
    };

    private final ObjectMapper objectMapper;

    public TradeExtraction parse(String llmContent) {
        String json = extractJson(llmContent);
        if (json == null || json.isBlank()) {
            log.warn("[TradeCapture] 응답에서 JSON 객체를 찾지 못함: {}", abbreviate(llmContent));
            return TradeExtraction.empty();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("[TradeCapture] LLM 응답 JSON 파싱 실패: {}", abbreviate(json), e);
            return TradeExtraction.empty();
        }

        String screenType = screenType(root);

        JsonNode trades = root.path("trades");
        if (!trades.isArray()) {
            log.warn("[TradeCapture] trades 배열이 없음: {}", abbreviate(json));
            return new TradeExtraction(screenType, List.of());
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

        if (result.isEmpty()) {
            // 0건은 정상 결과일 수도 실패일 수도 있다. 원인을 되짚으려면 원문이 남아 있어야 한다.
            log.warn("[TradeCapture] 추출 0건 - screenType: {}, 원문: {}", screenType, abbreviate(json));
        } else {
            log.debug("[TradeCapture] 추출 {}건 - screenType: {}", result.size(), screenType);
        }
        return new TradeExtraction(screenType, result);
    }

    private String screenType(JsonNode root) {
        String raw = root.path("screenType").asText(null);
        if (raw == null || raw.isBlank()) {
            // screenType 을 안 준 모델도 있다. 추출된 게 있으면 체결내역으로 보고 진행한다.
            return TradeExtraction.EXECUTION;
        }
        String upper = raw.trim().toUpperCase();
        return switch (upper) {
            case TradeExtraction.BALANCE, TradeExtraction.EXECUTION, TradeExtraction.NONE -> upper;
            default -> TradeExtraction.EXECUTION;
        };
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

    /**
     * 응답 문자열에서 JSON 객체를 꺼낸다.
     *
     * <p>예전에는 첫 '{' 부터 마지막 '}' 까지를 통째로 잘랐다. 모델이 설명을 붙이거나
     * 추론 블록을 내보내면 그 안의 중괄호까지 끌려들어와 JSON 전체가 깨졌고, 그러면
     * 제대로 읽어낸 표까지 통째로 버려졌다. 지금은 중괄호 균형을 세어 온전한 객체만 집는다.
     */
    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String text = THINK_BLOCK.matcher(content).replaceAll(" ").trim();
        if (text.isEmpty()) {
            return null;
        }

        String fallback = null;
        for (int i = text.indexOf('{'); i >= 0; i = text.indexOf('{', i + 1)) {
            String candidate = balancedObject(text, i);
            if (candidate == null) {
                // 여기서부터는 닫히지 않는다 — 응답이 중간에 잘린 경우가 대부분이다
                log.warn("[TradeCapture] 응답이 중간에 잘린 것으로 보임 (길이 {})", text.length());
                break;
            }
            if (candidate.contains("\"trades\"")) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    /** start 위치의 '{' 와 짝이 맞는 '}' 까지 잘라낸다. 끝까지 안 닫히면 null. */
    private String balancedObject(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
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
        if (s == null) {
            return "(없음)";
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }
}
