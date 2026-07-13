package com.investment.portal.domain.entity.fortune;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFortune {
    private Long fortuneId;
    private String ticker;         // 정식 티커 (US: AAPL, KR: 005930.KS)
    private LocalDate fortuneDate; // 운세 기준일 (KST)
    private String content;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}
