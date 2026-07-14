package com.investment.portal.application.service.insight;

import com.investment.portal.application.dto.insight.InsightResultResponse;
import com.investment.portal.application.dto.stock.DividendMonthRow;
import com.investment.portal.application.dto.stock.StockWithLatestPriceResponse;
import com.investment.portal.domain.entity.insight.InsightResult;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import com.investment.portal.domain.repository.dividend.DividendHistoryMapper;
import com.investment.portal.domain.repository.insight.InsightResultMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioItemMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import com.investment.portal.domain.repository.survey.SurveyMapper;
import com.investment.portal.infrastructure.messaging.InsightBuildProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwak.common.ai.AiGatewayClient;
import kwak.common.application.event.ActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

    private final InsightResultMapper       insightResultMapper;
    private final PortfolioMapper           portfolioMapper;
    private final PortfolioItemMapper       portfolioItemMapper;
    private final SurveyMapper              surveyMapper;
    private final StockPriceHistoryMapper   stockPriceHistoryMapper;
    private final PortfolioStockInfoProvider stockInfoProvider;
    private final DividendHistoryMapper      dividendHistoryMapper;
    private final DividendInsightBuilder     dividendInsightBuilder;
    private final AiGatewayClient           aiGatewayClient;
    private final CombinedInsightPromptBuilder promptBuilder;
    private final CombinedInsightParser     combinedParser;
    private final InsightBuildStatusService statusService;
    private final InsightBuildProducer      buildProducer;
    private final ApplicationEventPublisher eventPublisher;

    private static final Map<String, String> TYPE_TITLE = Map.of(
            "KEY_FINDINGS",              "주요 발견사항",
            "INVESTMENT_STYLE",          "나의 투자성향",
            "RISK_ASSESSMENT",           "리스크 평가",
            "PORTFOLIO_ALIGNMENT",       "포트폴리오 정합성",
            "INVESTMENT_RECOMMENDATION", "투자 추천",
            "STOCK_MBTI",                "투자 MBTI",
            "PROFILE_FIT",               "성향 적합도",
            "DIVIDEND_INSIGHT",          "배당 인사이트"
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
    public String requestBuild(String userId) {
        if (!statusService.tryAcquire(userId)) {
            log.info("[Insight] 이미 빌드 진행 중 - userId: {}", userId);
            return "ALREADY_PROCESSING";
        }
        buildProducer.publish(userId);
        eventPublisher.publishEvent(ActivityEvent.of(
                userId, "INSIGHT_BUILD_REQUEST", "INSIGHT", null, "인사이트 결과생성 요청"));
        return "PROCESSING";
    }

    @Override
    public void executeBuild(String userId) {
        List<PortfolioItem> allItems = portfolioMapper.findByUserId(userId).stream()
                .flatMap(p -> portfolioItemMapper.findByPortfolioId(p.getPortfolioId()).stream())
                .toList();
        Map<String, StockInfo> stockMap = allItems.isEmpty()
                ? Collections.emptyMap() : stockInfoProvider.fetchForItems(allItems);

        // ── 배당 컨텍스트 (DIVIDEND_INSIGHT + 통합 프롬프트 공용) ──
        List<String> tickers = allItems.stream().map(PortfolioItem::getStockCd)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, Set<Integer>> divMonths = tickers.isEmpty() ? Map.of()
                : DividendInsightBuilder.toMonthsMap(
                        dividendHistoryMapper.findDividendMonthsByStockCodes(tickers));
        String dividendBlock = dividendInsightBuilder.promptBlock(allItems, stockMap, divMonths);
        String surveyBlock = surveyBlock(userId);

        // ── 통합 LLM 1회 호출 ──
        CombinedInsight combined = callCombinedLlm(userId, allItems, stockMap, dividendBlock, surveyBlock);

        // ── LLM 4종 (없으면 규칙 폴백) ──
        String riskContent = (combined != null && !combined.riskLines().isEmpty())
                ? String.join("\n", combined.riskLines())
                : buildRiskAssessment(userId, allItems, stockMap).getContent();
        String alignContent = (combined != null && !combined.alignmentLines().isEmpty())
                ? String.join("\n", combined.alignmentLines())
                : buildPortfolioAlignment(userId, allItems, stockMap).getContent();
        String recoContent = (combined != null && !combined.recommendationLines().isEmpty())
                ? String.join("\n", combined.recommendationLines())
                : buildInvestmentRecommendation(userId, allItems, stockMap).getContent();
        String profileFitContent = (combined != null && combined.profileFitJson() != null)
                ? combined.profileFitJson()
                : buildProfileFitFallback(userId, allItems, stockMap);
        String dividendContent = dividendInsightBuilder.buildContent(
                allItems, stockMap, divMonths, surveyBlock,
                combined != null ? combined.dividendJson() : null);

        List<InsightResult> items = List.of(
                buildKeyFindings(userId),
                buildInvestmentStyle(userId, allItems, stockMap),
                buildItem(userId, "RISK_ASSESSMENT", riskContent),
                buildItem(userId, "PORTFOLIO_ALIGNMENT", alignContent),
                buildItem(userId, "INVESTMENT_RECOMMENDATION", recoContent),
                buildStockMbti(userId),
                buildItem(userId, "PROFILE_FIT", profileFitContent),
                buildItem(userId, "DIVIDEND_INSIGHT", dividendContent)
        );
        items.forEach(item -> {
            insightResultMapper.upsert(item);
            log.info("[Insight] upsert 완료 - userId: {}, type: {}", userId, item.getResultTypeCd());
        });
        eventPublisher.publishEvent(ActivityEvent.of(
                userId, "INSIGHT_BUILD_COMPLETE", "INSIGHT", null,
                "인사이트 " + items.size() + "종 생성 완료" + (combined != null ? " (LLM)" : " (규칙 폴백)")));
    }

    @Override
    public InsightResultResponse generateStockMbti(String userId) {
        InsightResult item = buildStockMbti(userId);   // 설문 점수 기반 규칙 계산 (LLM 미사용)
        insightResultMapper.upsert(item);
        log.info("[Insight] STOCK_MBTI 즉시 생성 - userId: {}", userId);
        return getResultByType(userId, "STOCK_MBTI");
    }

    /** 통합 프롬프트 구성 후 LLM 1회 호출 → 파싱. 실패 시 null. */
    private CombinedInsight callCombinedLlm(String userId,
                                            List<PortfolioItem> items,
                                            Map<String, StockInfo> stockMap,
                                            String dividendBlock,
                                            String surveyBlock) {
        if (items.isEmpty()) return null;
        List<StockInfo> infoList = new ArrayList<>(stockMap.values());
        if (infoList.isEmpty()) return null;

        long sectorCnt = infoList.stream().map(StockInfo::sector).distinct().count();
        double avgPE  = infoList.stream().filter(s -> s.peRatio() > 0).mapToDouble(StockInfo::peRatio).average().orElse(0);
        double avgDiv = infoList.stream().mapToDouble(StockInfo::dividendYield).average().orElse(0);
        double avgPos = infoList.stream().filter(s -> s.fiftyTwoWeekHigh() > s.fiftyTwoWeekLow())
                .mapToDouble(StockInfo::pricePosition).average().orElse(50);
        String metricsBlock = String.format("평균 PER: %.1f | 배당수익률: %.2f%% | 52주 평균 위치: %.0f%%",
                avgPE, avgDiv * 100, avgPos);
        String stockLines = infoList.stream().sorted(Comparator.comparing(StockInfo::ticker))
                .map(StockInfo::toContextLine).collect(Collectors.joining("\n"));

        InsightPromptContext ctx = new InsightPromptContext(
                items.size(), (int) sectorCnt, surveyBlock, metricsBlock, stockLines, dividendBlock);
        String raw = aiGatewayClient.generateContent(CombinedInsightPromptBuilder.SYSTEM_PROMPT, promptBuilder.build(ctx));
        if (raw == null) {
            log.warn("[Insight] 통합 LLM 응답 없음 - 규칙 폴백 - userId: {}", userId);
            return null;
        }
        CombinedInsight parsed = combinedParser.parse(raw);
        if (parsed == null) log.warn("[Insight] 통합 LLM 파싱 실패 - 규칙 폴백 - userId: {}", userId);
        return parsed;
    }

    /** 설문 점수 → 성향 코드 한 줄 블록 (투자 축만). 미완료 시 "설문 미완료". */
    private String surveyBlock(String userId) {
        List<Map<String, Object>> scores = surveyMapper.findRiskProfileScores(userId);
        if (scores.isEmpty()) return "설문 미완료";
        Map<String, Double> m = scores.stream().collect(Collectors.toMap(
                s -> String.valueOf(s.get("description")),
                s -> ((Number) s.get("score")).doubleValue(), (a, b) -> a));
        double profit = m.getOrDefault("수익추구", 50.0);
        double risk = m.getOrDefault("리스크허용", 50.0);
        double longTerm = m.getOrDefault("장기투자", 50.0);
        double div = m.getOrDefault("분산투자", 50.0);
        String code = (profit >= 62.5 ? "G" : "V") + (risk >= 62.5 ? "R" : "S")
                + (longTerm >= 62.5 ? "L" : "T") + (div >= 62.5 ? "D" : "F");
        return String.format("투자 성향 코드: %s\n수익추구 %.0f / 리스크허용 %.0f / 장기투자 %.0f / 분산투자 %.0f",
                code, profit, risk, longTerm, div);
    }

    /** PROFILE_FIT 규칙 기반 폴백 — JSON 문자열. */
    private String buildProfileFitFallback(String userId,
                                           List<PortfolioItem> items,
                                           Map<String, StockInfo> stockMap) {
        List<StockInfo> infoList = new ArrayList<>(stockMap.values());
        if (infoList.isEmpty()) {
            return "{\"fit\":[],\"rebalance\":[\"종목을 추가하면 성향 적합도 분석을 제공합니다.\"]}";
        }
        List<Map<String, Object>> scores = surveyMapper.findRiskProfileScores(userId);
        double riskTol = scores.stream()
                .filter(s -> "리스크허용".equals(String.valueOf(s.get("description"))))
                .map(s -> ((Number) s.get("score")).doubleValue()).findFirst().orElse(50.0);

        List<Map<String, Object>> fit = new ArrayList<>();
        for (StockInfo s : infoList) {
            String level;
            String reason;
            if (s.peRatio() > 25 && riskTol < StockMbtiContentBuilder.AXIS_CUT) {
                level = "낮음"; reason = "고PER 성장주로 안정 성향 대비 변동성이 큽니다.";
            } else if (s.peRatio() > 25) {
                level = "보통"; reason = "성장주 특성으로 수익 성향과 부합하나 변동성에 유의하세요.";
            } else {
                level = "높음"; reason = "밸류에이션 부담이 낮아 보유 성향과 무난합니다.";
            }
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("ticker", s.companyName() != null ? s.companyName() : s.ticker());
            entry.put("level", level);
            entry.put("reason", reason);
            fit.add(entry);
        }
        List<String> rebalance = new ArrayList<>();
        long growth = infoList.stream().filter(x -> x.peRatio() > 25).count();
        if (riskTol < StockMbtiContentBuilder.AXIS_CUT && growth * 2 > infoList.size()) rebalance.add("고PER 성장주 비중을 줄이고 배당주로 안정성을 보강하세요.");
        if (items.size() < 5) rebalance.add("보유 종목이 적습니다. 분산을 위해 5종목 이상을 권장합니다.");
        if (rebalance.isEmpty()) rebalance.add("현재 구성은 성향과 대체로 부합합니다. 정기 리밸런싱을 유지하세요.");

        try {
            ObjectMapper om = new ObjectMapper();
            return om.writeValueAsString(Map.of("fit", fit, "rebalance", rebalance));
        } catch (Exception e) {
            return "{\"fit\":[],\"rebalance\":[\"분석을 일시적으로 제공할 수 없습니다.\"]}";
        }
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

            // 성격 축(EI 등)이 섞이면 평균·최강/최약 축 문구가 오염되므로 투자 축만 남긴다
            scoreMap.keySet().retainAll(StockMbtiContentBuilder.INVEST_AXES);

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
     * STOCK_MBTI(V2): 성격 4축(EI/SN/TF/JP) + 투자 4축 → 일반 MBTI + 투자 MBTI 16유형.
     * 산출 로직은 StockMbtiContentBuilder 순수 함수 참조.
     */
    private InsightResult buildStockMbti(String userId) {
        List<Map<String, Object>> surveyScores = surveyMapper.findRiskProfileScores(userId);

        if (surveyScores.isEmpty()) {
            log.info("[Insight] STOCK_MBTI - 설문 미완료 - userId: {}", userId);
            return buildItem(userId, "STOCK_MBTI", StockMbtiContentBuilder.MISSING_CONTENT);
        }

        Map<String, Double> scoreMap = surveyScores.stream()
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("description")),
                        m -> ((Number) m.get("score")).doubleValue(),
                        (a, b) -> a));

        String content = StockMbtiContentBuilder.build(scoreMap);
        log.info("[Insight] STOCK_MBTI - userId: {}, code: {}/{}",
                userId, content.split("\n")[1], content.split("\n")[3]);
        return buildItem(userId, "STOCK_MBTI", content);
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

        // ── 수치 계산 (규칙 기반) ──
        double avgPos = infoList.stream()
                .filter(s -> s.fiftyTwoWeekHigh() > s.fiftyTwoWeekLow())
                .mapToDouble(StockInfo::pricePosition).average().orElse(50.0);
        long   negCnt = infoList.stream().filter(s -> s.changePercent() < 0).count();

        // ── 규칙 기반 ──
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

        // ── 규칙 기반 ──
        String stockLines = infoList.stream().sorted(Comparator.comparing(StockInfo::ticker))
                .map(StockInfo::toContextLine).collect(Collectors.joining("\n"));
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

        // ── 규칙 기반 ──
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
