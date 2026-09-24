package com.investment.portal.application.service.qa;

import com.investment.analyzer.market_analyzer.domain.entity.dividend.DividendHistory;
import com.investment.analyzer.market_analyzer.domain.repository.dividend.DividendHistoryMapper;
import com.investment.portal.application.dto.qa.MarketNewsRow;
import com.investment.portal.application.dto.qa.MarketQuestionRequest;
import com.investment.portal.application.dto.qa.MarketQuestionResponse;
import com.investment.portal.application.dto.stock.StockPriceMoveRow;
import com.investment.portal.application.service.trade.StockResolver;
import com.investment.portal.domain.entity.history.stockPrice.StockPriceHistory;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import com.investment.portal.domain.repository.stock.StockRef;
import kwak.common.ai.AiGatewayClient;
import kwak.common.application.event.ActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 고정 5개 질문 유형에 답한다. 로컬 LLM은 "의도 추출"과 "서술" 두 번만 부르고,
 * 실제 수치는 전부 결정적인 매퍼 쿼리로 계산한다 — TradeCaptureServiceImpl 이
 * 매매기록 추출에서 LLM에게 숫자를 그대로 믿지 않는 것과 같은 이유다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketQuestionServiceImpl implements MarketQuestionService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    static final String ACTION_TYPE = "AI_QA_ASK";
    static final String TARGET_TYPE = "QUESTION_TYPE";
    static final String OUTCOME_FAILED = "FAILED";

    private final AiGatewayClient aiGatewayClient;
    private final StockResolver stockResolver;
    private final PeriodHintResolver periodHintResolver;
    private final QuestionIntentParser intentParser;
    private final StockPriceHistoryMapper stockPriceHistoryMapper;
    private final DividendHistoryMapper dividendHistoryMapper;
    private final MarketNewsLookupService marketNewsLookupService;
    private final ApplicationEventPublisher eventPublisher;

    /** 처리 결과(판정된 질문 유형, 장애면 FAILED)와 함께 질문 원문을 활동로그에 남긴다 —
     * UNKNOWN 으로 떨어진 질문을 모아 봐야 지원 유형·트리거를 넓힐 근거가 생긴다. */
    @Override
    public MarketQuestionResponse ask(String userId, MarketQuestionRequest request) {
        String outcome = OUTCOME_FAILED;
        try {
            MarketQuestionResponse response = answer(request);
            outcome = response.questionType();
            return response;
        } finally {
            eventPublisher.publishEvent(ActivityEvent.of(
                    userId, ACTION_TYPE, TARGET_TYPE, outcome, request.text()));
        }
    }

    private MarketQuestionResponse answer(MarketQuestionRequest request) {
        ExtractedQuestionIntent extracted = intentParser.parse(
                askLlm(QuestionIntentExtractionPrompt.SYSTEM, QuestionIntentExtractionPrompt.USER_PREFIX + request.text()));
        QuestionType type = QuestionType.fromRaw(extracted.questionType());

        if (type == QuestionType.UNKNOWN) {
            return softAnswer(null, QuestionType.UNKNOWN,
                    "질문을 이해하지 못했어요. 예: '올해 애플이 가장 많이 하락한 날 무슨 일이 있었는지' 처럼 물어봐 주세요.");
        }

        StockResolver.Resolution resolution = stockResolver.resolve(extracted.stockName(), null);
        if (!resolution.isResolved()) {
            return softAnswer(null, type, unresolvedStockMessage(extracted.stockName(), resolution.candidates()));
        }

        String stockCd = resolution.stockCd();
        String stockNm = resolution.stockNm();
        PeriodHintResolver.DateRange range = periodHintResolver.resolve(extracted.periodHint());

        String facts = null;
        LocalDate newsTargetDate = null;

        switch (type) {
            case MAX_DROP_DAY -> {
                Optional<StockPriceMoveRow> row =
                        stockPriceHistoryMapper.findMaxChangeDay(stockCd, range.start(), range.end(), "DROP");
                if (row.isEmpty()) {
                    return noPriceData(stockNm, type);
                }
                facts = moveFacts(stockNm, row.get(), "하락");
                newsTargetDate = row.get().getPriceDt();
            }
            case MAX_GAIN_DAY -> {
                Optional<StockPriceMoveRow> row =
                        stockPriceHistoryMapper.findMaxChangeDay(stockCd, range.start(), range.end(), "GAIN");
                if (row.isEmpty()) {
                    return noPriceData(stockNm, type);
                }
                facts = moveFacts(stockNm, row.get(), "상승");
                newsTargetDate = row.get().getPriceDt();
            }
            case PERIOD_RETURN -> {
                Optional<StockPriceHistory> startRow = stockPriceHistoryMapper.findClosestOnOrAfter(stockCd, range.start());
                Optional<StockPriceHistory> endRow = stockPriceHistoryMapper.findClosestOnOrBefore(stockCd, range.end());
                if (startRow.isEmpty() || endRow.isEmpty()) {
                    return noPriceData(stockNm, type);
                }
                facts = returnFacts(stockNm, startRow.get(), endRow.get());
            }
            case PERIOD_HIGH_LOW -> {
                Optional<StockPriceHistory> high = stockPriceHistoryMapper.findPeriodHighDay(stockCd, range.start(), range.end());
                Optional<StockPriceHistory> low = stockPriceHistoryMapper.findPeriodLowDay(stockCd, range.start(), range.end());
                if (high.isEmpty() && low.isEmpty()) {
                    return noPriceData(stockNm, type);
                }
                facts = highLowFacts(stockNm, high, low);
            }
            case DIVIDEND_SUMMARY -> {
                List<DividendHistory> dividends = dividendHistoryMapper.findRecentByStockCd(stockCd, 8);
                if (dividends.isEmpty()) {
                    return softAnswer(stockNm, type, "%s 종목의 배당 이력을 찾지 못했어요.".formatted(stockNm));
                }
                facts = dividendFacts(stockNm, dividends);
            }
            default -> {
                return softAnswer(stockNm, type, "이 질문은 아직 지원하지 않아요.");
            }
        }

        String newsSection = "";
        if (newsTargetDate != null) {
            List<MarketNewsRow> news = marketNewsLookupService.findNewsForDate(stockCd, stockNm, newsTargetDate);
            newsSection = newsFacts(news);
        }

        String userMessage = "FACTS:\n" + facts + (newsSection.isBlank() ? "" : "\n\nNEWS:\n" + newsSection);
        String answer = askLlm(MarketAnswerPrompt.SYSTEM, userMessage);
        return new MarketQuestionResponse(type.name(), stockNm,
                answer == null || answer.isBlank() ? "지금은 답변을 만들지 못했어요. 잠시 후 다시 시도해 주세요." : answer.trim());
    }

    /** ai 모듈 호출 실패는 매매기록 추출과 동일하게 흡수하지 않고 503로 올린다 —
     * "지금은 못 읽었다"를 사용자에게 알려야 하기 때문이다. */
    private String askLlm(String system, String user) {
        try {
            AiGatewayClient.ChatResponse res = aiGatewayClient.chat(system, user);
            return res == null ? null : res.content();
        } catch (Exception e) {
            log.error("[MarketQuestion] AI 모듈 호출 실패", e);
            throw new MarketQuestionUnavailableException(e);
        }
    }

    private String unresolvedStockMessage(String stockName, List<StockRef> candidates) {
        if (candidates.isEmpty()) {
            return "'%s' 종목을 찾지 못했어요. 정확한 종목명이나 티커로 다시 알려주세요."
                    .formatted(stockName == null ? "" : stockName);
        }
        String names = candidates.stream().map(StockRef::stockNm).collect(Collectors.joining(", "));
        return "'%s'과 비슷한 종목이 여러 개예요: %s. 어느 종목인지 다시 말씀해 주세요.".formatted(stockName, names);
    }

    private MarketQuestionResponse noPriceData(String stockNm, QuestionType type) {
        return softAnswer(stockNm, type,
                "%s 종목의 해당 기간 가격 데이터를 찾지 못했어요. 다른 기간으로 다시 물어봐 주세요.".formatted(stockNm));
    }

    private MarketQuestionResponse softAnswer(String stockNm, QuestionType type, String answer) {
        return new MarketQuestionResponse(type.name(), stockNm, answer);
    }

    // ── 사실(facts) 조립 — 전부 순수 문자열 포맷팅, LLM 호출 없음 ──────────────────────

    private String moveFacts(String stockNm, StockPriceMoveRow row, String direction) {
        return """
                - 종목: %s
                - 날짜: %s
                - 등락 방향: %s
                - 전일 종가: %s
                - 당일 종가: %s
                - 등락률: %s%%
                """.formatted(
                stockNm, row.getPriceDt().format(DATE_FMT), direction,
                row.getPrevClose(), row.getClosePrice(), row.getChangePct());
    }

    private String returnFacts(String stockNm, StockPriceHistory start, StockPriceHistory end) {
        BigDecimal startPrice = start.getClosePrice();
        BigDecimal endPrice = end.getClosePrice();
        BigDecimal returnPct = startPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : endPrice.subtract(startPrice)
                        .divide(startPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
        return """
                - 종목: %s
                - 시작일: %s (종가 %s)
                - 종료일: %s (종가 %s)
                - 기간 수익률: %s%%
                """.formatted(
                stockNm, start.getPriceDt().format(DATE_FMT), startPrice,
                end.getPriceDt().format(DATE_FMT), endPrice, returnPct);
    }

    private String highLowFacts(String stockNm, Optional<StockPriceHistory> high, Optional<StockPriceHistory> low) {
        StringBuilder sb = new StringBuilder("- 종목: ").append(stockNm).append("\n");
        high.ifPresent(h -> sb.append("- 최고가: ").append(h.getHighPrice())
                .append(" (").append(h.getPriceDt().format(DATE_FMT)).append(")\n"));
        low.ifPresent(l -> sb.append("- 최저가: ").append(l.getLowPrice())
                .append(" (").append(l.getPriceDt().format(DATE_FMT)).append(")\n"));
        return sb.toString();
    }

    private String dividendFacts(String stockNm, List<DividendHistory> dividends) {
        StringBuilder sb = new StringBuilder("- 종목: ").append(stockNm).append("\n- 최근 배당 이력:\n");
        for (DividendHistory d : dividends) {
            sb.append("  · ").append(d.getExDate().format(DATE_FMT)).append(": ").append(d.getDividend()).append("\n");
        }
        return sb.toString();
    }

    private String newsFacts(List<MarketNewsRow> news) {
        if (news.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (MarketNewsRow n : news) {
            sb.append(i++).append(". ").append(n.getTitle());
            if (n.getSource() != null) {
                sb.append(" (").append(n.getSource()).append(")");
            }
            sb.append("\n");
            if (n.getContent() != null && !n.getContent().isBlank()) {
                sb.append("   ").append(n.getContent()).append("\n");
            }
        }
        return sb.toString();
    }
}
