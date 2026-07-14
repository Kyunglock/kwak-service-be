package com.investment.analyzer.market_analyzer.api.controller;

import com.investment.analyzer.market_analyzer.application.dto.news.MarketBriefingResponse;
import com.investment.analyzer.market_analyzer.application.service.news.NewsService;
import kwak.common.application.dto.RokResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock NewsService newsService;
    @InjectMocks NewsController controller;

    @Test
    void 브리핑을_200으로_감싸_반환한다() {
        MarketBriefingResponse briefing =
                new MarketBriefingResponse(LocalDate.of(2026, 7, 14), "요약", "POSITIVE", List.of());
        when(newsService.getMarketBriefing()).thenReturn(briefing);

        ResponseEntity<?> res = controller.getMarketBriefing();

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(((RokResponse<?>) res.getBody()).getData()).isEqualTo(briefing);
    }

    @Test
    void 요약이_없으면_data_null_200() {
        when(newsService.getMarketBriefing()).thenReturn(null);

        ResponseEntity<?> res = controller.getMarketBriefing();

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(((RokResponse<?>) res.getBody()).getData()).isNull();
    }
}
