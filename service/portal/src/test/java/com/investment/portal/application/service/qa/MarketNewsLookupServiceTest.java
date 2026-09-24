package com.investment.portal.application.service.qa;

import com.investment.analyzer.market_analyzer.domain.repository.news.NewsMapper;
import com.investment.portal.application.dto.qa.MarketNewsRow;
import com.investment.portal.domain.repository.stock.StockResolveMapper;
import kwak.common.collector.CollectorGatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** DB 우선 → 없으면 collector 온디맨드 크롤링 폴백 로직 전담 테스트. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketNewsLookupServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 11);

    @Mock NewsMapper newsMapper;
    @Mock CollectorGatewayClient collectorGatewayClient;
    @Mock StockResolveMapper stockResolveMapper;

    private MarketNewsLookupService svc() {
        return new MarketNewsLookupService(newsMapper, collectorGatewayClient, stockResolveMapper);
    }

    @BeforeEach
    void setUp() {
        when(stockResolveMapper.findKoreanNameByTicker("AAPL")).thenReturn(Optional.of("애플"));
        when(stockResolveMapper.findKoreanNameByTicker("005930.KS")).thenReturn(Optional.empty());
    }

    @Test
    void DB에서_찾으면_collector를_부르지_않는다() {
        MarketNewsRow row = MarketNewsRow.builder().title("애플 급락").source("Reuters").build();
        when(newsMapper.findByDateAndKeyword(eq(DATE), any(), anyList())).thenReturn(List.of(row));

        List<MarketNewsRow> result = svc().findNewsForDate("AAPL", "Apple Inc.", DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("애플 급락");
        verifyNoInteractions(collectorGatewayClient);
    }

    @Test
    void 미국_종목은_약식_영문명_티커_한글명으로_찾고_다음날_새벽_마감시각을_기준으로_삼는다() {
        when(newsMapper.findByDateAndKeyword(any(), any(), anyList())).thenReturn(List.of(
                MarketNewsRow.builder().title("Apple shares fall").build()));

        svc().findNewsForDate("AAPL", "Apple Inc.", DATE);

        verify(newsMapper).findByDateAndKeyword(
                DATE, LocalDateTime.of(2026, 3, 12, 5, 0), List.of("Apple", "AAPL", "애플"));
    }

    @Test
    void 국내_종목은_종목명과_코드로_찾고_당일_15시30분_마감시각을_기준으로_삼는다() {
        when(newsMapper.findByDateAndKeyword(any(), any(), anyList())).thenReturn(List.of(
                MarketNewsRow.builder().title("삼성전자 급락").build()));

        svc().findNewsForDate("005930.KS", "삼성전자", DATE);

        verify(newsMapper).findByDateAndKeyword(
                DATE, LocalDateTime.of(2026, 3, 11, 15, 30), List.of("삼성전자", "005930"));
    }

    @Test
    void DB에_없으면_collector에_약식_영문명으로_온디맨드_크롤링을_요청한다() {
        when(newsMapper.findByDateAndKeyword(eq(DATE), any(), anyList())).thenReturn(List.of());
        CollectorGatewayClient.CrawlResult crawlResult = new CollectorGatewayClient.CrawlResult(
                true,
                List.of(new CollectorGatewayClient.CrawledArticle(
                        "Apple shares tumble", "Google News", "https://news.google.com/x",
                        "공급망 경고로 급락", "2026-03-11T14:30:00Z")),
                1);
        when(collectorGatewayClient.crawlOnDemand("Apple", "AAPL", DATE)).thenReturn(crawlResult);

        List<MarketNewsRow> result = svc().findNewsForDate("AAPL", "Apple Inc.", DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Apple shares tumble");
        assertThat(result.get(0).getContent()).isEqualTo("공급망 경고로 급락");
        verify(collectorGatewayClient).crawlOnDemand("Apple", "AAPL", DATE);
    }

    @Test
    void 쓸_만한_키워드가_없으면_DB는_건너뛰고_크롤링만_시도한다() {
        // 빈 키워드 목록이면 SQL 의 OR 괄호가 비어 문법 오류가 난다
        when(stockResolveMapper.findKoreanNameByTicker("F")).thenReturn(Optional.empty());
        when(collectorGatewayClient.crawlOnDemand(any(), any(), eq(DATE)))
                .thenReturn(new CollectorGatewayClient.CrawlResult(false, List.of(), 0));

        svc().findNewsForDate("F", "F", DATE);

        verifyNoInteractions(newsMapper);
        verify(collectorGatewayClient).crawlOnDemand("F", "F", DATE);
    }

    @Test
    void DB와_크롤링_둘_다_없으면_빈_목록() {
        when(newsMapper.findByDateAndKeyword(eq(DATE), any(), anyList())).thenReturn(List.of());
        when(collectorGatewayClient.crawlOnDemand(any(), any(), eq(DATE)))
                .thenReturn(new CollectorGatewayClient.CrawlResult(false, List.of(), 0));

        assertThat(svc().findNewsForDate("AAPL", "Apple Inc.", DATE)).isEmpty();
    }

    @Test
    void collector_호출이_예외를_던져도_예외가_새지_않고_빈_목록으로_흡수된다() {
        when(newsMapper.findByDateAndKeyword(eq(DATE), any(), anyList())).thenReturn(List.of());
        when(collectorGatewayClient.crawlOnDemand(any(), any(), eq(DATE)))
                .thenThrow(new RuntimeException("timeout"));

        assertThat(svc().findNewsForDate("AAPL", "Apple Inc.", DATE)).isEmpty();
    }
}
