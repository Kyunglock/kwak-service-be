package com.investment.portal.application.dto.fortune;

import com.investment.portal.domain.entity.fortune.StockFortune;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FortuneResponse(String ticker, LocalDate fortuneDate, String content, LocalDateTime regDt) {

    public static FortuneResponse from(StockFortune fortune) {
        return new FortuneResponse(fortune.getTicker(), fortune.getFortuneDate(),
                fortune.getContent(), fortune.getRegDt());
    }
}
