package com.investment.analyzer.market_analyzer.application.service.news;

import com.investment.analyzer.market_analyzer.application.dto.news.MarketBriefingResponse;
import com.investment.analyzer.market_analyzer.domain.entity.news.MarketSummary;
import com.investment.analyzer.market_analyzer.domain.entity.news.NewsArticle;
import com.investment.analyzer.market_analyzer.domain.repository.news.NewsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock NewsMapper newsMapper;
    @InjectMocks NewsServiceImpl service;

    private static final LocalDate DT = LocalDate.of(2026, 7, 14);

    private MarketSummary summary() {
        return MarketSummary.builder()
                .summaryDt(DT).summary("S&P500은 상승했다.").articleCount(2).build();
    }

    private NewsArticle article(String title) {
        return NewsArticle.builder()
                .id(1L).title(title).source("Google News US Market")
                .url("https://news.example.com/" + title)
                .publishedAt(LocalDateTime.of(2026, 7, 14, 5, 0)).build();
    }

    @Test
    void 요약과_기사를_조합해_반환한다() {
        when(newsMapper.findLatestSummary()).thenReturn(Optional.of(summary()));
        when(newsMapper.findArticlesForBriefing(DT)).thenReturn(List.of(article("Fed rate")));

        MarketBriefingResponse res = service.getMarketBriefing();

        assertThat(res.summaryDt()).isEqualTo(DT);
        assertThat(res.summary()).isEqualTo("S&P500은 상승했다.");
        assertThat(res.articles()).hasSize(1);
        assertThat(res.articles().get(0).title()).isEqualTo("Fed rate");
    }

    @Test
    void 요약이_없으면_null을_반환하고_기사는_조회하지_않는다() {
        when(newsMapper.findLatestSummary()).thenReturn(Optional.empty());

        assertThat(service.getMarketBriefing()).isNull();
        verify(newsMapper, never()).findArticlesForBriefing(any());
    }

    @Test
    void 캐시_TTL_내_재호출은_매퍼를_다시_조회하지_않는다() {
        when(newsMapper.findLatestSummary()).thenReturn(Optional.of(summary()));
        when(newsMapper.findArticlesForBriefing(DT)).thenReturn(List.of());

        service.getMarketBriefing();
        service.getMarketBriefing();

        verify(newsMapper, times(1)).findLatestSummary();
    }

    @Test
    void 요약_없음_결과도_캐시되어_반복_조회하지_않는다() {
        when(newsMapper.findLatestSummary()).thenReturn(Optional.empty());

        service.getMarketBriefing();
        service.getMarketBriefing();

        verify(newsMapper, times(1)).findLatestSummary();
    }
}
