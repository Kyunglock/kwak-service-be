package com.investment.portal.application.service.insight;

import java.util.List;

public record CombinedInsight(
        String profileFitJson,
        List<String> riskLines,
        List<String> alignmentLines,
        List<String> recommendationLines
) {}
