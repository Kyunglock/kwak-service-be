package com.investment.portal.application.service.trade;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LLM이 뽑아낸 원시 추출 결과 한 건. 아직 검증되지 않았다 —
 * ticker 는 LLM이 말한 것일 뿐이고, 저장 가능한 값인지는 이후 단계에서 판정한다.
 */
record ExtractedTrade(
        String name,
        String ticker,
        String type,
        BigDecimal qty,
        BigDecimal price,
        BigDecimal amount,
        LocalDate date,
        String currency
) {}
