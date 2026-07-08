package com.investment.portal.application.service.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.investment.portal.application.dto.stock.DividendMonthRow;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DIVIDEND_INSIGHT 수치 계산 + 규칙 폴백 + LLM 서술 병합.
 * 수치 필드는 항상 서버 계산값이며, LLM JSON은 summary/profileContrast/findings 서술만 반영한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DividendInsightBuilder {

    /** FE CurrencyContext.EXCHANGE_RATE와 동일한 고정 환율 — 혼합 통화 합산에만 사용 */
    static final double USD_KRW = 1500.0;

    /** 배당주 판정 기준: 배당수익률 2% 이상 (소액 배당 대형주를 배당주로 치지 않기 위함) */
    static final double DIVIDEND_STOCK_MIN_YIELD = 0.02;

    private static final Pattern SURVEY_CODE = Pattern.compile("성향 코드: ([GV][RS][LT])");

    private final ObjectMapper objectMapper;

    public static Map<String, Set<Integer>> toMonthsMap(List<DividendMonthRow> rows) {
        Map<String, Set<Integer>> map = new HashMap<>();
        for (DividendMonthRow r : rows) {
            if (r.getStockCd() == null || r.getDivMonth() == null) continue;
            map.computeIfAbsent(r.getStockCd(), k -> new TreeSet<>()).add(r.getDivMonth());
        }
        return map;
    }

    /** 통합 프롬프트에 삽입할 배당 컨텍스트 한 블록. */
    public String promptBlock(List<PortfolioItem> items, Map<String, StockInfo> stockMap,
                              Map<String, Set<Integer>> monthsByStock) {
        Computed c = compute(items, stockMap, monthsByStock);
        if (c.stockCount == 0) return "배당 컨텍스트: 보유 종목 없음";
        return String.format(
                "배당 컨텍스트: 배당주(수익률 2%% 이상) 비중 %.0f%% | 포트폴리오 배당수익률 %.2f%% | 월별 지급 종목 수(1~12월) %s",
                c.dividendStockWeight, c.portfolioYield, Arrays.toString(c.monthlyFlow));
    }

    /** 최종 content JSON — 수치는 계산값, 서술은 llmJson이 유효하면 덮어쓴다. */
    public String buildContent(List<PortfolioItem> items, Map<String, StockInfo> stockMap,
                               Map<String, Set<Integer>> monthsByStock,
                               String surveyBlock, String llmJson) {
        Computed c = compute(items, stockMap, monthsByStock);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("summary", c.stockCount == 0
                ? "종목을 추가하면 배당 분석을 제공합니다."
                : String.format("보유 %d종목 중 %d종목이 배당을 지급하며, 연 예상 배당은 %s입니다.",
                        c.stockCount, c.payerCount, formatAnnual(c)));
        root.put("annualDividendUsd", round2(c.annualUsd));
        root.put("annualDividendKrw", Math.round(c.annualKrw));
        root.put("portfolioYield", round2(c.portfolioYield));
        root.put("dividendStockWeight", round1(c.dividendStockWeight));
        root.put("profileContrast", profileContrast(surveyBlock, c));
        ArrayNode flow = root.putArray("monthlyFlow");
        for (int v : c.monthlyFlow) flow.add(v);
        ArrayNode amounts = root.putArray("monthlyAmountsKrw");
        for (double v : c.monthlyAmountKrw) amounts.add(Math.round(v));
        ArrayNode findings = root.putArray("findings");
        for (String f : buildFindings(c)) findings.add(f);

        overlayLlmNarration(root, llmJson);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("[Insight] DIVIDEND_INSIGHT 직렬화 실패: {}", e.getMessage());
            return "{\"summary\":\"배당 분석 생성에 실패했습니다.\",\"annualDividendUsd\":0,\"annualDividendKrw\":0,"
                 + "\"portfolioYield\":0,\"dividendStockWeight\":0,\"profileContrast\":\"\","
                 + "\"monthlyFlow\":[0,0,0,0,0,0,0,0,0,0,0,0],"
                 + "\"monthlyAmountsKrw\":[0,0,0,0,0,0,0,0,0,0,0,0],\"findings\":[]}";
        }
    }

    // ── 내부 계산 ──────────────────────────────────────────────────────────────

    private record Computed(int stockCount, int payerCount,
                            double annualUsd, double annualKrw,
                            double portfolioYield, double dividendStockWeight,
                            int[] monthlyFlow, double[] monthlyAmountKrw,
                            String topPayer, int krwNoDivCount) {}

    private Computed compute(List<PortfolioItem> items, Map<String, StockInfo> stockMap,
                             Map<String, Set<Integer>> monthsByStock) {
        Map<String, Double> qtyByCode = new LinkedHashMap<>();
        for (PortfolioItem i : items) {
            if (i.getStockCd() == null || i.getHoldQty() == null) continue;
            qtyByCode.merge(i.getStockCd(), i.getHoldQty().doubleValue(), Double::sum);
        }

        int payerCount = 0;
        double totalKrw = 0, divStockValueKrw = 0, annualUsd = 0, annualKrw = 0, annualAllKrw = 0;
        int[] monthly = new int[12];
        double[] monthlyAmt = new double[12];
        String topPayer = null;
        double topPayerKrw = -1;
        int krwNoDivCount = 0;

        int stockCount = 0;
        for (Map.Entry<String, Double> e : qtyByCode.entrySet()) {
            StockInfo s = stockMap.get(e.getKey());
            if (s == null || s.currentPrice() <= 0) continue;
            stockCount++;
            double qty = e.getValue();
            double toKrw = "KRW".equals(s.currency()) ? 1.0 : USD_KRW;
            double valueKrw = s.currentPrice() * qty * toKrw;
            totalKrw += valueKrw;

            double annualNative = s.dividendYield() * s.currentPrice() * qty;   // 연 배당(원 통화)
            if ("KRW".equals(s.currency()) && annualNative <= 0) krwNoDivCount++;
            if (annualNative <= 0) continue;

            payerCount++;
            // 배당주 비중은 "수익률 2% 이상" 종목만 집계 (소액 배당 대형주 제외)
            if (s.dividendYield() >= DIVIDEND_STOCK_MIN_YIELD) divStockValueKrw += valueKrw;
            if ("KRW".equals(s.currency())) annualKrw += annualNative;
            else annualUsd += annualNative;
            double annualAsKrw = annualNative * toKrw;
            annualAllKrw += annualAsKrw;
            if (annualAsKrw > topPayerKrw) {
                topPayerKrw = annualAsKrw;
                topPayer = s.ticker();
            }
            Set<Integer> months = monthsByStock.getOrDefault(e.getKey(), Set.of());
            for (int m : months) {
                if (m >= 1 && m <= 12) {
                    monthly[m - 1]++;
                    monthlyAmt[m - 1] += annualAsKrw / months.size();   // 연 배당을 지급월에 균등 분배(예상치)
                }
            }
        }

        double yield  = totalKrw > 0 ? annualAllKrw / totalKrw * 100.0 : 0.0;
        double weight = totalKrw > 0 ? divStockValueKrw / totalKrw * 100.0 : 0.0;
        return new Computed(stockCount, payerCount, annualUsd, annualKrw, yield, weight, monthly, monthlyAmt, topPayer, krwNoDivCount);
    }

    private static final String KRW_COVERAGE_CAVEAT = "국내 종목 배당은 집계에서 제외될 수 있습니다.";

    private List<String> buildFindings(Computed c) {
        List<String> out = new ArrayList<>();
        if (c.payerCount == 0) {
            if (c.stockCount > 0) out.add("보유 종목 중 배당 지급 종목이 없습니다.");
            if (c.krwNoDivCount > 0) out.add(KRW_COVERAGE_CAVEAT);
            return out;
        }
        List<String> gapMonths = new ArrayList<>();
        for (int m = 0; m < 12; m++) if (c.monthlyFlow[m] == 0) gapMonths.add(String.valueOf(m + 1));
        if (!gapMonths.isEmpty() && gapMonths.size() < 12) {
            out.add("월 배당 공백: " + String.join("·", gapMonths) + "월");
        }
        if (c.portfolioYield < 1.5)      out.add(String.format("포트폴리오 배당수익률 %.2f%%로 낮은 편입니다.", c.portfolioYield));
        else if (c.portfolioYield <= 3)  out.add(String.format("포트폴리오 배당수익률 %.2f%%로 S&P500 평균 수준입니다.", c.portfolioYield));
        else                             out.add(String.format("포트폴리오 배당수익률 %.2f%%로 높은 편입니다.", c.portfolioYield));
        if (c.topPayer != null) out.add("연 배당 기여 1위: " + c.topPayer);
        if (c.krwNoDivCount > 0) out.add(KRW_COVERAGE_CAVEAT);
        return out;
    }

    /**
     * 성향 코드와 배당주(수익률 2%+) 비중 대조.
     * 1번째 글자 G/V(수익추구)가 1차 축 — 수익추구 성향이면 배당주 비중이 낮은 게 일관,
     * 수익추구가 낮으면(V) 배당·안정 선호로 보아 비중이 높은 게 일관. S(리스크 허용 낮음)는 보조 축.
     */
    private String profileContrast(String surveyBlock, Computed c) {
        if (c.stockCount == 0) return "";
        Matcher m = surveyBlock == null ? null : SURVEY_CODE.matcher(surveyBlock);
        if (m == null || !m.find()) {
            return "설문을 완료하면 투자 성향과 배당 구성을 대조해 드립니다.";
        }
        String code = m.group(1);
        boolean growth = code.charAt(0) == 'G';   // 수익추구 높음
        boolean stable = code.charAt(1) == 'S';   // 리스크 허용 낮음 → 안정 지향
        String band = c.dividendStockWeight < 20 ? "낮음" : c.dividendStockWeight <= 60 ? "중간" : "높음";
        String pct = String.format("%.0f%%", c.dividendStockWeight);

        if (growth && band.equals("높음"))
            return "수익추구 성향인데 배당주 비중이 " + pct + "로 높습니다. 성장주 중심 성향과는 거리가 있는 구성입니다.";
        if (growth && band.equals("낮음"))
            return "수익추구 성향과 배당주 비중 " + pct + " — 성향대로 성장주 중심의 구성입니다.";
        if (!growth && stable && band.equals("낮음"))
            return "안정 지향 성향이지만 배당주 비중이 " + pct + "로 낮아, 현금흐름 안정성 측면에서 성향과 차이가 있습니다.";
        if (!growth && band.equals("높음"))
            return "안정·배당 선호 성향과 배당주 비중 " + pct + "가 잘 맞는 구성입니다.";
        return "성향 코드 " + code + " 기준으로 배당주 비중 " + pct + "는 무난한 수준입니다.";
    }

    /** LLM 서술(summary/profileContrast/findings)만 덮어쓴다 — 수치는 무시. */
    private void overlayLlmNarration(ObjectNode root, String llmJson) {
        if (llmJson == null) return;
        try {
            JsonNode llm = objectMapper.readTree(llmJson);
            if (llm.hasNonNull("summary") && !llm.get("summary").asText().isBlank())
                root.put("summary", llm.get("summary").asText());
            if (llm.hasNonNull("profileContrast") && !llm.get("profileContrast").asText().isBlank())
                root.put("profileContrast", llm.get("profileContrast").asText());
            JsonNode f = llm.get("findings");
            if (f != null && f.isArray() && !f.isEmpty()) {
                List<String> filtered = new ArrayList<>();
                f.forEach(n -> {
                    if (n.isTextual() && !n.asText().isBlank()) filtered.add(n.asText());
                });
                if (!filtered.isEmpty()) {
                    ArrayNode arr = root.putArray("findings");
                    filtered.forEach(arr::add);
                }
            }
        } catch (Exception e) {
            log.warn("[Insight] DIVIDEND_INSIGHT LLM 서술 병합 실패 - 템플릿 유지: {}", e.getMessage());
        }
    }

    private String formatAnnual(Computed c) {
        List<String> parts = new ArrayList<>();
        if (c.annualUsd > 0) parts.add(String.format("약 $%.2f", c.annualUsd));
        if (c.annualKrw > 0) parts.add(String.format("약 ₩%,d", Math.round(c.annualKrw)));
        return parts.isEmpty() ? "없음(무배당)" : String.join(" + ", parts);
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
