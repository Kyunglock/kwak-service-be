package com.investment.portal.application.service.qa;

import com.investment.analyzer.market_analyzer.domain.repository.news.NewsMapper;
import com.investment.portal.application.dto.qa.MarketNewsRow;
import kwak.common.collector.CollectorGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** DB 우선 → 없으면 collector 온디맨드 크롤링 폴백 로직 전담 테스트. */
@ExtendWith(MockitoExtension.class)
class MarketNewsLookupServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 11);

    @Mock NewsMapper newsMapper;
    @Mock CollectorGatewayClient collectorGatewayClient;

    private MarketNewsLookupService svc() {
        return new MarketNewsLookupService(newsMapper, collectorGatewayClient);
    }

    @Test
    void DB에서_찾으면_collector를_부르지_않는다() {
        MarketNewsRow row = MarketNewsRow.builder().title("애플 급락").source("Reuters").build();
        when(newsMapper.findByDateAndKeyword(eq(DATE), anyList())).thenReturn(List.of(row));

        List<MarketNewsRow> result = svc().findNewsForDate("AAPL", "애플", DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("애플 급락");
        verifyNoInteractions(collectorGatewayClient);
    }

    @Test
    void DB에_없으면_collector_온디맨드_크롤링으로_폴백한다() {
        when(newsMapper.findByDateAndKeyword(eq(DATE), anyList())).thenReturn(List.of());
        CollectorGatewayClient.CrawlResult crawlResult = new CollectorGatewayClient.CrawlResult(
                true,
                List.of(new CollectorGatewayClient.CrawledArticle(
                        "Apple shares tumble", "Google News", "https://news.google.com/x",
                        "공급망 경고로 급락", "2026-03-11T14:30:00Z")),
                1);
        when(collectorGatewayClient.crawlOnDemand("애플", "AAPL", DATE)).thenReturn(crawlResult);

        List<MarketNewsRow> result = svc().findNewsForDate("AAPL", "애플", DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Apple shares tumble");
        assertThat(result.get(0).getContent()).isEqualTo("공급망 경고로 급락");
        verify(collectorGatewayClient).crawlOnDemand("애플", "AAPL", DATE);
    }

    @Test
    void DB와_크롤링_둘_다_없으면_빈_목록() {
        when(newsMapper.findByDateAndKeyword(eq(DATE), anyList())).thenReturn(List.of());
        when(collectorGatewayClient.crawlOnDemand(any(), any(), eq(DATE)))
                .thenReturn(new CollectorGatewayClient.CrawlResult(false, List.of(), 0));

        assertThat(svc().findNewsForDate("AAPL", "애플", DATE)).isEmpty();
    }

    @Test
    void collector_호출이_예외를_던져도_예외가_새지_않고_빈_목록으로_흡수된다() {
        when(newsMapper.findByDateAndKeyword(eq(DATE), anyList())).thenReturn(List.of());
        when(collectorGatewayClient.crawlOnDemand(any(), any(), eq(DATE)))
                .thenThrow(new RuntimeException("timeout"));

        List<MarketNewsRow> result = svc().findNewsForDate("AAPL", "애플", DATE);

        assertThat(result).isEmpty();
    }
}
