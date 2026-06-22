package com.investment.portal.application.service.insight;

import com.investment.portal.application.dto.insight.InsightResultResponse;
import com.investment.portal.application.dto.stock.StockWithLatestPriceResponse;
import com.investment.portal.domain.entity.insight.InsightResult;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import com.investment.portal.domain.repository.insight.InsightResultMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioItemMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import com.investment.portal.domain.repository.survey.SurveyMapper;
import com.investment.portal.infrastructure.external.openai.InsightOpenAiClient;
import com.investment.portal.infrastructure.external.yahoo.StockInfo;
import com.investment.portal.infrastructure.external.yahoo.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final InsightResultMapper     insightResultMapper;
    private final PortfolioMapper         portfolioMapper;
    private final PortfolioItemMapper     portfolioItemMapper;
    private final SurveyMapper            surveyMapper;
    private final StockPriceHistoryMapper stockPriceHistoryMapper;
    private final YahooFinanceClient      yahooFinanceClient;
    private final InsightOpenAiClient     openAiClient;

    private static final String SYSTEM_PROMPT =
            "당신은 한국어로 응답하는 주식 포트폴리오 분석 전문가입니다. " +
            "주어진 포트폴리오 데이터를 바탕으로 구체적이고 실용적인 분석을 제공하세요. " +
            "반드시 다음 JSON 형식으로만 응답하세요: {\"lines\": [\"분석 내용1\", \"분석 내용2\", ...]} " +
            "각 항목은 완결된 한국어 문장으로 작성하며, 4~6개 항목을 구성하세요. " +
            "구체적인 수치와 근거를 포함하고 실행 가능한 인사이트를 제공하세요.";

    private static final Map<String, String> TYPE_TITLE = Map.of(
            "KEY_FINDINGS",              "주요 발견사항",
            "INVESTMENT_STYLE",          "나의 투자성향",
            "RISK_ASSESSMENT",           "리스크 평가",
            "PORTFOLIO_ALIGNMENT",       "포트폴리오 정합성",
            "INVESTMENT_RECOMMENDATION", "투자 추천",
            "STOCK_MBTI",                "투자 MBTI"
    );

    // ── 조회 ──────────────────────────────────────────────────────────────────

    @Override
    public List<InsightResultResponse> getAllResults(String userId) {
        return insightResultMapper.findAllByUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public InsightResultResponse getResultByType(String userId, String resultTypeCd) {
        InsightResult r = insightResultMapper.findByUserIdAndType(userId, resultTypeCd);
        return r == null ? null : toResponse(r);
    }

    // ── 빌드 ──────────────────────────────────────────────────────────────────

    @Override
    public List<InsightResultResponse> buildAndSaveContext(String userId) {

        List<PortfolioItem> allItems = portfolioMapper.findByUserId(userId).stream()
                .flatMap(p -> portfolioItemMapper.findByPortfolioId(p.getPortfolioId()).stream())
                .toList();

        List<String> tickers = allItems.stream()
                .map(PortfolioItem::getStockCd).distinct().toList();

        log.info("[Insight] 종목 조회 시작 - userId: {}, tickers: {}", userId, tickers);

        Map<String, StockInfo> stockMap = tickers.isEmpty()
                ? Collections.emptyMap()
                : yahooFinanceClient.fetchBatch(tickers);

        log.info("[Insight] Yahoo Finance 조회 완료 - 성공: {}건 / 요청: {}건", stockMap.size(), tickers.size());

        List<InsightResult> items = List.of(
                buildKeyFindings(userId),
                buildInvestmentStyle(userId, allItems, stockMap),
                buildRiskAssessment(userId, allItems, stockMap),
                buildPortfolioAlignment(userId, allItems, stockMap),
                buildInvestmentRecommendation(userId, allItems, stockMap),
                buildStockMbti(userId)
        );

        items.forEach(item -> {
            insightResultMapper.upsert(item);
            log.info("[Insight] upsert 완료 - userId: {}, type: {}", userId, item.getResultTypeCd());
        });

        return getAllResults(userId);
    }

    // ── 타입별 컨텍스트 빌더 ──────────────────────────────────────────────────

    /**
     * KEY_FINDINGS: collector가 수집한 tbl_stock_price_history + tbl_companies 데이터 기반.
     * Yahoo Finance 직접 호출을 제거하여 crumb/쿠키 인증 실패 문제를 해소한다.
     */
    private InsightResult buildKeyFindings(String userId) {
        List<Map<String, Object>> topStocks = portfolioItemMapper.findTopStocksByHolderCount(10);
        List<String> popularTickers = topStocks.stream()
                .map(m -> String.valueOf(m.get("stockCd")).toUpperCase())
                .toList();

        if (popularTickers.isEmpty()) {
            return buildItem(userId, "KEY_FINDINGS",
                    "아직 포트폴리오 데이터가 없습니다. 종목을 추가한 후 다시 시도해 주세요.");
        }

        // collector DB에서 전체 종목 최신 가격·섹터 조회 후 인기 종목 필터링
        Set<String> popularSet = new HashSet<>(popularTickers);
        List<StockWithLatestPriceResponse> infoList = stockPriceHistoryMapper.findAllWithLatestPrice()
                .stream()
                .filter(s -> s.getStockCd() != null && popularSet.contains(s.getStockCd().toUpperCase()))
                .toList();

        if (infoList.isEmpty()) {
            log.warn("[Insight] KEY_FINDINGS - Collector DB에 인기 종목 데이터 없음 - tickers: {}", popularTickers);
            return buildItem(userId, "KEY_FINDINGS",
                    "시장 데이터를 준비 중입니다. 잠시 후 다시 시도해 주세요.");
        }

        List<String> bullets = new ArrayList<>();

        // 섹터 집중도 분석
        List<StockWithLatestPriceResponse> sectorList = infoList.stream()
                .filter(s -> s.getSector() != null && !s.getSector().isBlank()
                        && !"Unknown".equalsIgnoreCase(s.getSector()))
                .toList();

        sectorList.stream()
                .collect(Collectors.groupingBy(StockWithLatestPriceResponse::getSector, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(top -> {
                    int pct = (int) (top.getValue() * 100L / infoList.size());
                    String label = sectorList.stream()
                            .filter(s -> top.getKey().equals(s.getSector()))
                            .map(s -> s.getSectorKo() != null && !s.getSectorKo().isBlank()
                                    ? s.getSectorKo() : s.getSector())
                            .findFirst().orElse(top.getKey());
                    bullets.add(String.format(
                            "%s 섹터 선호도가 인기 종목의 %d%%를 차지하며 투자자들의 섹터 선호가 집중되고 있습니다.",
                            label, pct));
                });

        // 당일 등락 분석 (종가 vs 시가)
        List<StockWithLatestPriceResponse> movable = infoList.stream()
                .filter(s -> s.getOpenPrice() != null && s.getClosePrice() != null
                        && s.getOpenPrice().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (!movable.isEmpty()) {
            double avgChange = movable.stream()
                    .mapToDouble(s -> s.getClosePrice().subtract(s.getOpenPrice())
                            .divide(s.getOpenPrice(), 6, RoundingMode.HALF_UP)
                            .doubleValue() * 100)
                    .average().orElse(0);

            String sentiment = avgChange >= 1.0 ? "강한 상승 모멘텀"
                    : avgChange >= 0          ? "소폭 상승 흐름" : "하락 압력";
            bullets.add(String.format(
                    "인기 종목 평균 당일 등락률 %+.2f%%로 %s이 형성되고 있습니다.", avgChange, sentiment));

            long risingCount = movable.stream()
                    .filter(s -> s.getClosePrice().compareTo(s.getOpenPrice()) > 0)
                    .count();
            int risingPct = (int) (risingCount * 100L / movable.size());
            bullets.add(risingPct >= 60
                    ? String.format("인기 종목의 %d%%가 당일 상승 마감하며 시장 심리가 낙관적으로 형성되었습니다.", risingPct)
                    : risingPct >= 40
                    ? String.format("인기 종목의 %d%%가 상승 마감하며 혼조세 장세가 이어지고 있습니다.", risingPct)
                    : String.format("인기 종목의 %d%%만 상승 마감하여 하락 압력이 우세합니다.", risingPct));
        }

        // 거래량 분석
        infoList.stream()
                .filter(s -> s.getVolume() != null)
                .mapToDouble(s -> (double) s.getVolume())
                .average()
                .ifPresent(avg -> {
                    if (avg > 1_000_000) {
                        bullets.add(String.format(
                                "인기 종목 평균 거래량 %.0f만주로 활발한 시장 참여가 확인됩니다.", avg / 10_000));
                    }
                });

        if (bullets.isEmpty()) {
            bullets.add("현재 시장 데이터를 분석 중입니다.");
        }

        log.info("[Insight] KEY_FINDINGS 생성 완료 - userId: {}, bullets: {}건", userId, bullets.size());
        return buildItem(userId, "KEY_FINDINGS", String.join("\n", bullets));
    }

    /**
     * INVESTMENT_STYLE: 설문 점수 기반 AI 의견 생성, 설문 미완료시 포트폴리오 fallback
     * content 포맷: 첫 줄 = 유형명, 두 번째 줄 = 종합 의견
     */
    private InsightResult buildInvestmentStyle(String userId,
                                               List<PortfolioItem> items,
                                               Map<String, StockInfo> stockMap) {

        List<Map<String, Object>> surveyScores = surveyMapper.findRiskProfileScores(userId);

        if (!surveyScores.isEmpty()) {
            Map<String, Double> scoreMap = surveyScores.stream()
                    .collect(Collectors.toMap(
                            m -> String.valueOf(m.get("description")),
                            m -> ((Number) m.get("score")).doubleValue(),
                            (a, b) -> a));

            double avgScore = scoreMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(50.0);

            String typeName = avgScore >= 70 ? "공격 성장형"
                    : avgScore >= 50        ? "중위험 성장형"
                    :                         "안정 추구형";

            String opinion = generateInvestmentOpinion(scoreMap, avgScore);

            log.info("[Insight] 설문 기반 투자성향 - userId: {}, type: {}", userId, typeName);
            return buildItem(userId, "INVESTMENT_STYLE", typeName + "\n" + opinion);
        }

        log.info("[Insight] 설문 미완료 - 주식 데이터 기반 fallback - userId: {}", userId);

        if (stockMap.isEmpty()) {
            return buildItem(userId, "INVESTMENT_STYLE",
                    "설문 미완료\n설문을 완료하면 나의 투자 성향이 분석됩니다.");
        }

        List<StockInfo> infoList = new ArrayList<>(stockMap.values());
        double avgPE = infoList.stream().filter(s -> s.peRatio() > 0)
                .mapToDouble(StockInfo::peRatio).average().orElse(0);
        double avgDividend = infoList.stream().mapToDouble(StockInfo::dividendYield).average().orElse(0);
        String typeName = avgPE > 30 ? "성장주 중심형" : avgPE > 15 ? "균형 성장형" : "가치투자형";
        String dividendLabel = avgDividend > 0.03 ? "배당 수익 중시" : avgDividend > 0.01 ? "중립적 배당" : "저배당 성향";
        String topSector = infoList.stream()
                .collect(Collectors.groupingBy(s -> s.sector().isBlank() ? "Unknown" : s.sector(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("분석 불가");
        String description = String.format("%s · 선호 섹터: %s · 평균 PER: %.1f · 배당수익률: %.2f%%",
                dividendLabel, topSector, avgPE, avgDividend * 100);
        return buildItem(userId, "INVESTMENT_STYLE", typeName + "\n" + description);
    }

    /**
     * STOCK_MBTI: 3차원 설문 점수 → G/V · R/S · L/T 조합으로 8가지 투자 유형 분류
     * content 포맷 (줄 구분): {code}\n{name}\n{description}\n{profitScore}:{riskScore}:{longTermScore}
     */
    private InsightResult buildStockMbti(String userId) {
        List<Map<String, Object>> surveyScores = surveyMapper.findRiskProfileScores(userId);

        if (surveyScores.isEmpty()) {
            log.info("[Insight] STOCK_MBTI - 설문 미완료 - userId: {}", userId);
            return buildItem(userId, "STOCK_MBTI",
                    "설문 미완료\n투자 MBTI 분석 불가\n투자 성향 설문을 완료하면 나만의 투자 MBTI를 확인할 수 있습니다.\n0:0:0");
        }

        Map<String, Double> scoreMap = surveyScores.stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("description")),
                        m -> ((Number) m.get("score")).doubleValue(),
                        (a, b) -> a));

        double profitScore   = scoreMap.getOrDefault("수익추구", 50.0);
        double riskScore     = scoreMap.getOrDefault("리스크허용", 50.0);
        double longTermScore = scoreMap.getOrDefault("장기투자", 50.0);

        String g = profitScore   >= 50 ? "G" : "V";
        String r = riskScore     >= 50 ? "R" : "S";
        String l = longTermScore >= 50 ? "L" : "T";
        String code = g + r + l;

        String[] meta = getMbtiMeta(code);
        String scores = String.format("%.0f:%.0f:%.0f", profitScore, riskScore, longTermScore);

        log.info("[Insight] STOCK_MBTI - userId: {}, code: {}", userId, code);
        return buildItem(userId, "STOCK_MBTI", code + "\n" + meta[0] + "\n" + meta[1] + "\n" + scores);
    }

    private String[] getMbtiMeta(String code) {
        return switch (code) {
            case "GRL" -> new String[]{"성장 개척자",
                    "공격적인 수익 추구와 높은 리스크 감내력으로 장기 성장 자산에 집중합니다. 기술주·성장주에 과감하게 투자하며 시장 변동에도 장기 원칙을 고수하는 유형입니다."};
            case "GRT" -> new String[]{"모멘텀 헌터",
                    "높은 수익을 위해 적극적으로 시장 모멘텀을 활용합니다. 리스크를 감내하며 단기 트레이딩 기회를 포착하는 공격적인 트레이더입니다."};
            case "GSL" -> new String[]{"균형 성장가",
                    "수익성을 추구하되 리스크를 신중히 관리하며 장기적 안목으로 포트폴리오를 구성합니다. 성장성과 안정성의 균형을 추구하는 현명한 투자자입니다."};
            case "GST" -> new String[]{"신중한 수익가",
                    "수익 목표가 뚜렷하지만 리스크에 민감하며 단기 성과를 중시합니다. 안전마진을 확보하면서도 수익 기회를 놓치지 않으려는 유형입니다."};
            case "VRL" -> new String[]{"가치 탐험가",
                    "안정적 자산을 선호하면서도 과감한 리스크를 감수하며 장기 가치를 발굴합니다. 저평가 종목을 발굴해 오랜 기간 보유하는 역발상 가치 투자자입니다."};
            case "VRT" -> new String[]{"역발상 트레이더",
                    "시장의 반대 방향을 주목하며 단기 반전 기회를 포착합니다. 리스크를 감내하면서 역발상 투자로 알파를 창출하는 독자적인 스타일입니다."};
            case "VSL" -> new String[]{"배당 수호자",
                    "안정성을 최우선으로 리스크를 최소화하며 장기 배당 수익에 집중합니다. 꾸준한 현금흐름과 자산 보존을 중시하는 가장 안정적인 투자 성향입니다."};
            case "VST" -> new String[]{"안전 수익가",
                    "안정성과 자산 보존을 중시하며 단기 저위험 수익을 추구합니다. 급격한 시장 변동을 피하고 안전한 단기 투자 기회를 선호하는 유형입니다."};
            default     -> new String[]{"분석 중", "투자 성향 데이터를 분석하는 중입니다."};
        };
    }

    private String generateInvestmentOpinion(Map<String, Double> scoreMap, double avgScore) {
        double profitSeek    = scoreMap.getOrDefault("수익추구", avgScore);
        double riskTolerance = scoreMap.getOrDefault("리스크허용", avgScore);
        double longTerm      = scoreMap.getOrDefault("장기투자", avgScore);

        String profitPart = profitSeek >= 70
                ? "높은 수익을 적극적으로 추구하며"
                : profitSeek >= 40
                ? "수익성과 안정성의 균형을 추구하며"
                : "원금 보존을 우선시하며";

        String riskPart = riskTolerance >= 70
                ? "상당한 리스크를 감내할 수 있고"
                : riskTolerance >= 40
                ? "적정 수준의 리스크를 관리하며"
                : "리스크를 최소화하는 방향으로";

        String longTermPart = longTerm >= 70
                ? "장기적 관점에서 성장 자산에 집중하는 투자 성향입니다."
                : longTerm >= 40
                ? "중장기적 시각으로 포트폴리오를 운용하는 투자 성향입니다."
                : "단기 안정성과 자산 보존을 중시하는 투자 성향입니다.";

        double maxScore = scoreMap.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double minScore = scoreMap.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);

        String highlight = "";
        if (maxScore - minScore >= 25) {
            String strongDim = scoreMap.entrySet().stream().max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("");
            String weakDim = scoreMap.entrySet().stream().min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("");
            highlight = String.format(" 특히 %s 성향이 두드러지며, %s 영역에서 보완이 필요합니다.",
                    strongDim, weakDim);
        }

        return profitPart + " " + riskPart + " " + longTermPart + highlight;
    }

    private InsightResult buildRiskAssessment(String userId,
                                              List<PortfolioItem> items,
                                              Map<String, StockInfo> stockMap) {
        if (items.isEmpty()) {
            return buildItem(userId, "RISK_ASSESSMENT", "포트폴리오 종목이 없어 리스크 평가를 수행할 수 없습니다.");
        }

        List<StockInfo> infoList = new ArrayList<>(stockMap.values());

        // ── 수치 계산 (AI 프롬프트 컨텍스트 + 폴백용 공용) ──
        double avgPos = infoList.stream()
                .filter(s -> s.fiftyTwoWeekHigh() > s.fiftyTwoWeekLow())
                .mapToDouble(StockInfo::pricePosition).average().orElse(50.0);
        double avgPE  = infoList.stream().filter(s -> s.peRatio() > 0)
                .mapToDouble(StockInfo::peRatio).average().orElse(0);
        double avgDiv = infoList.stream().mapToDouble(StockInfo::dividendYield).average().orElse(0);
        long   negCnt = infoList.stream().filter(s -> s.changePercent() < 0).count();
        long   highCnt = infoList.stream().filter(s -> s.pricePosition() > 80).count();
        long   lowCnt  = infoList.stream().filter(s -> s.pricePosition() < 20).count();

        Map<String, Long> sectorMap = infoList.stream()
                .collect(Collectors.groupingBy(s -> s.sector().isBlank() ? "Unknown" : s.sector(), Collectors.counting()));
        String topSector = sectorMap.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("데이터 없음");
        int topSectorPct = infoList.isEmpty() ? 0
                : (int) (sectorMap.getOrDefault(topSector, 0L) * 100L / infoList.size());

        // ── AI 호출 ──
        String userPrompt = String.format(
                "[포트폴리오 리스크 평가 요청]\n" +
                "보유 종목: %d개 | 섹터: %d개\n" +
                "집중 섹터: %s (%d%%)\n" +
                "52주 가격 위치: 평균 %.0f%% (고점 근처 %d개 / 저점 근처 %d개)\n" +
                "하락 종목 비율: %.0f%%\n" +
                "평균 PER: %.1f | 배당수익률: %.2f%%\n" +
                "종목 목록:\n%s\n\n" +
                "이 포트폴리오의 전반적인 리스크 수준, 주요 위험 요인, 단기·중기 주의사항을 분석해주세요.",
                items.size(), sectorMap.size(), topSector, topSectorPct,
                avgPos, highCnt, lowCnt,
                infoList.isEmpty() ? 0 : negCnt * 100.0 / infoList.size(),
                avgPE, avgDiv * 100,
                infoList.stream().map(StockInfo::toContextLine).collect(Collectors.joining("\n"))
        );

        List<String> aiLines = openAiClient.chatLines(SYSTEM_PROMPT, userPrompt);
        if (aiLines != null) {
            log.info("[Insight] RISK_ASSESSMENT AI 생성 완료 - userId: {}", userId);
            return buildItem(userId, "RISK_ASSESSMENT", String.join("\n", aiLines));
        }

        // ── 폴백: 규칙 기반 ──
        String priceLabel   = avgPos > 70 ? "52주 고점 근처 — 고평가 주의"
                : avgPos > 40 ? "52주 중간대 — 적정 수준" : "52주 저점 근처 — 저평가 가능성";
        String divLabel     = items.size() >= 10 ? "충분한 분산 투자"
                : items.size() >= 5 ? "적정 분산" : "집중 투자 (분산 권장)";
        String sentimentLabel = infoList.isEmpty() ? "데이터 부족"
                : negCnt > infoList.size() / 2 ? "단기 하락 종목 다수" : "단기 상승 종목 다수";
        return buildItem(userId, "RISK_ASSESSMENT", String.format(
                "분산도: %s (%d종목)\n가격 위치: %s (평균 %.0f%%)\n시장 흐름: %s",
                divLabel, items.size(), priceLabel, avgPos, sentimentLabel));
    }

    private InsightResult buildPortfolioAlignment(String userId,
                                                  List<PortfolioItem> items,
                                                  Map<String, StockInfo> stockMap) {
        if (items.isEmpty()) return buildItem(userId, "PORTFOLIO_ALIGNMENT", "포트폴리오 종목이 없습니다.");

        List<StockInfo> infoList = new ArrayList<>(stockMap.values());
        long   sectorCnt    = infoList.stream().map(StockInfo::sector).distinct().count();
        long   dividendCnt  = infoList.stream().filter(s -> s.dividendYield() > 0.01).count();
        long   growthCnt    = infoList.stream().filter(s -> s.peRatio() > 25).count();
        double avgPE        = infoList.stream().filter(s -> s.peRatio() > 0)
                .mapToDouble(StockInfo::peRatio).average().orElse(0);
        int    growthPct    = infoList.isEmpty() ? 0 : (int) (growthCnt * 100L / infoList.size());
        int    dividendPct  = infoList.isEmpty() ? 0 : (int) (dividendCnt * 100L / infoList.size());

        // 설문 기반 성향 정보
        List<Map<String, Object>> surveyScores = surveyMapper.findRiskProfileScores(userId);
        String mbtiBlock = "";
        if (!surveyScores.isEmpty()) {
            Map<String, Double> scoreMap = surveyScores.stream().collect(Collectors.toMap(
                    m -> String.valueOf(m.get("description")),
                    m -> ((Number) m.get("score")).doubleValue(), (a, b) -> a));
            double profit   = scoreMap.getOrDefault("수익추구", 50.0);
            double risk     = scoreMap.getOrDefault("리스크허용", 50.0);
            double longTerm = scoreMap.getOrDefault("장기투자", 50.0);
            String code = (profit >= 50 ? "G" : "V") + (risk >= 50 ? "R" : "S") + (longTerm >= 50 ? "L" : "T");
            mbtiBlock = String.format(
                    "투자 성향 코드: %s\n수익추구 %.0f / 리스크허용 %.0f / 장기투자 %.0f (각 100점 만점)\n",
                    code, profit, risk, longTerm);
        }

        String topSectors = infoList.stream()
                .collect(Collectors.groupingBy(s -> s.sector().isBlank() ? "Unknown" : s.sector(), Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3).map(e -> e.getKey() + "(" + e.getValue() + "종목)")
                .collect(Collectors.joining(", "));

        // ── AI 호출 ──
        String stockLines = infoList.stream().sorted(Comparator.comparing(StockInfo::ticker))
                .map(StockInfo::toContextLine).collect(Collectors.joining("\n"));
        String userPrompt = String.format(
                "[포트폴리오 정합성 분석 요청]\n%s" +
                "실제 포트폴리오:\n" +
                "- 종목 %d개 / 섹터 %d개\n" +
                "- 성장주(PER 25↑): %d%% / 배당주(수익률 1%%↑): %d%%\n" +
                "- 평균 PER: %.1f\n" +
                "- 주요 섹터: %s\n" +
                "종목 목록:\n%s\n\n" +
                "투자 성향과 실제 포트폴리오 구성의 일치 여부, 괴리가 있다면 어떤 조정이 필요한지 분석해주세요.",
                mbtiBlock, items.size(), sectorCnt,
                growthPct, dividendPct, avgPE, topSectors, stockLines
        );

        List<String> aiLines = openAiClient.chatLines(SYSTEM_PROMPT, userPrompt);
        if (aiLines != null) {
            log.info("[Insight] PORTFOLIO_ALIGNMENT AI 생성 완료 - userId: {}", userId);
            return buildItem(userId, "PORTFOLIO_ALIGNMENT", String.join("\n", aiLines));
        }

        // ── 폴백 ──
        double diversityPct = (double) sectorCnt / items.size() * 100;
        String alignScore = diversityPct > 60 ? "높음 (우수)" : diversityPct > 30 ? "보통 (개선 가능)" : "낮음 (집중 해소 필요)";
        return buildItem(userId, "PORTFOLIO_ALIGNMENT", String.format(
                "포트폴리오 정합성: %s\n보유 섹터: %d개 | 종목: %d개\n배당주: %d종목 · 성장주(PER25↑): %d종목\n\n[종목 상세]\n%s",
                alignScore, sectorCnt, items.size(), dividendCnt, growthCnt, stockLines));
    }

    private InsightResult buildInvestmentRecommendation(String userId,
                                                        List<PortfolioItem> items,
                                                        Map<String, StockInfo> stockMap) {
        if (items.isEmpty()) return buildItem(userId, "INVESTMENT_RECOMMENDATION",
                "포트폴리오 종목이 없습니다. 종목을 추가한 후 맞춤 추천을 받아보세요.");

        List<StockInfo> infoList = new ArrayList<>(stockMap.values());
        double avgPE  = infoList.stream().filter(s -> s.peRatio() > 0)
                .mapToDouble(StockInfo::peRatio).average().orElse(0);
        double avgDiv = infoList.stream().mapToDouble(StockInfo::dividendYield).average().orElse(0);
        double avgPos = infoList.stream().filter(s -> s.fiftyTwoWeekHigh() > s.fiftyTwoWeekLow())
                .mapToDouble(StockInfo::pricePosition).average().orElse(50);

        Map<String, Long> sectorMap = infoList.stream()
                .collect(Collectors.groupingBy(s -> s.sector().isBlank() ? "Unknown" : s.sector(), Collectors.counting()));
        String topSector    = sectorMap.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("데이터 없음");
        int    topSectorPct = infoList.isEmpty() ? 0
                : (int) (sectorMap.getOrDefault(topSector, 0L) * 100L / infoList.size());

        // 설문 기반 투자 성향
        List<Map<String, Object>> surveyScores = surveyMapper.findRiskProfileScores(userId);
        String mbtiInfo = "설문 미완료";
        if (!surveyScores.isEmpty()) {
            Map<String, Double> scoreMap = surveyScores.stream().collect(Collectors.toMap(
                    m -> String.valueOf(m.get("description")),
                    m -> ((Number) m.get("score")).doubleValue(), (a, b) -> a));
            double profit   = scoreMap.getOrDefault("수익추구", 50.0);
            double risk     = scoreMap.getOrDefault("리스크허용", 50.0);
            double longTerm = scoreMap.getOrDefault("장기투자", 50.0);
            String code = (profit >= 50 ? "G" : "V") + (risk >= 50 ? "R" : "S") + (longTerm >= 50 ? "L" : "T");
            String[] meta = getMbtiMeta(code);
            mbtiInfo = code + " " + meta[0] + " (수익추구 " + (int)profit + " / 리스크허용 " + (int)risk + " / 장기투자 " + (int)longTerm + ")";
        }

        // ── AI 호출 ──
        String userPrompt = String.format(
                "[투자 전략 추천 요청]\n" +
                "투자자 성향: %s\n\n" +
                "현재 포트폴리오:\n" +
                "- 종목 %d개 / 섹터 %d개\n" +
                "- 집중 섹터: %s (%d%%)\n" +
                "- 평균 PER: %.1f | 배당수익률: %.2f%%\n" +
                "- 52주 평균 가격 위치: %.0f%% (100%%=52주 고점)\n" +
                "종목 목록:\n%s\n\n" +
                "이 투자자에게 맞는 구체적인 포트폴리오 개선 전략, 추가 편입 검토 섹터/유형, 리밸런싱 방향을 추천해주세요.",
                mbtiInfo, items.size(), sectorMap.size(),
                topSector, topSectorPct, avgPE, avgDiv * 100, avgPos,
                infoList.stream().map(StockInfo::toContextLine).collect(Collectors.joining("\n"))
        );

        List<String> aiLines = openAiClient.chatLines(SYSTEM_PROMPT, userPrompt);
        if (aiLines != null) {
            log.info("[Insight] INVESTMENT_RECOMMENDATION AI 생성 완료 - userId: {}", userId);
            return buildItem(userId, "INVESTMENT_RECOMMENDATION", String.join("\n", aiLines));
        }

        // ── 폴백: 규칙 기반 ──
        List<String> recs = new ArrayList<>();
        if (items.size() < 5) recs.add("보유 종목이 " + items.size() + "개로 적습니다. 분산을 위해 5종목 이상을 권장합니다.");
        sectorMap.entrySet().stream()
                .filter(e -> !infoList.isEmpty() && (double) e.getValue() / infoList.size() > 0.5)
                .forEach(e -> recs.add(String.format("%s 섹터에 %.0f%%가 집중되어 있습니다. 다른 섹터로의 분산을 검토하세요.",
                        e.getKey(), (double) e.getValue() / infoList.size() * 100)));
        if (avgDiv < 0.01) recs.add("평균 배당수익률이 낮습니다. 안정적 현금흐름을 원한다면 배당주 편입을 고려하세요.");
        if (avgPE > 40) recs.add(String.format("평균 PER이 %.0f으로 높습니다. 밸류에이션 부담 구간이므로 분할 매수 전략을 권장합니다.", avgPE));
        if (avgPos > 75) recs.add("보유 종목 대부분이 52주 고점 근처에 있습니다. 신규 매수보다 비중 유지 전략을 권장합니다.");
        if (recs.isEmpty()) recs.add("현재 포트폴리오 구성은 전반적으로 양호합니다. 정기적인 리밸런싱으로 비중을 유지하세요.");
        String content = IntStream.range(0, recs.size())
                .mapToObj(i -> (i + 1) + ". " + recs.get(i)).collect(Collectors.joining("\n"));
        return buildItem(userId, "INVESTMENT_RECOMMENDATION", content);
    }

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private InsightResult buildItem(String userId, String typeCd, String content) {
        return InsightResult.builder()
                .userId(userId).resultTypeCd(typeCd)
                .title(TYPE_TITLE.get(typeCd)).content(content).build();
    }

    private InsightResultResponse toResponse(InsightResult r) {
        return new InsightResultResponse(
                r.getResultId(), r.getUserId(), r.getResultTypeCd(),
                r.getTitle(), r.getContent(), r.getRegDt(), r.getUpdDt());
    }
}
