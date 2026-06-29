package com.investment.portal.application.service.insight;

public record InsightPromptContext(
        int itemCount,
        int sectorCount,
        String surveyBlock,
        String metricsBlock,
        String stockLines
) {}
