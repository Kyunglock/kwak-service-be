package com.investment.portal.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.portal.application.service.insight.*;
import com.investment.portal.infrastructure.messaging.InsightBuildProducer;
import kwak.common.ai.AiGatewayClient;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.mock;

final class InsightServiceTestFactory {
    private InsightServiceTestFactory() {}

    /** requestBuild 검증용: 빌드에 쓰이지 않는 의존성은 mock으로 채운다. */
    static InsightServiceImpl withAsyncDeps(InsightBuildStatusService status, InsightBuildProducer producer) {
        ObjectMapper om = new ObjectMapper();
        return new InsightServiceImpl(
                mock(com.investment.portal.domain.repository.insight.InsightResultMapper.class),
                mock(com.investment.portal.domain.repository.portfolio.PortfolioMapper.class),
                mock(com.investment.portal.domain.repository.portfolio.PortfolioItemMapper.class),
                mock(com.investment.portal.domain.repository.survey.SurveyMapper.class),
                mock(com.investment.portal.domain.repository.stock.StockPriceHistoryMapper.class),
                mock(PortfolioStockInfoProvider.class),
                mock(com.investment.portal.domain.repository.dividend.DividendHistoryMapper.class),
                new DividendInsightBuilder(om),
                mock(AiGatewayClient.class),
                new CombinedInsightPromptBuilder(),
                new CombinedInsightParser(om),
                status,
                producer,
                mock(ApplicationEventPublisher.class)
        );
    }
}
