package com.investment.portal.domain.entity.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItem {
    
    private Long itemId;              // 항목ID
    private Long portfolioId;         // 포트폴리오ID
    private String stockCd;           // 종목코드
    private BigDecimal holdQty;       // 보유수량
    private BigDecimal buyPrice;      // 매수단가
    private LocalDate buyDt;          // 매수일자
    private BigDecimal buyAmount;     // 매수금액 (보유수량 * 매수단가)
    private String currency;          // 통화
    private String memo;              // 메모
    private String useYn;             // 사용여부
    private LocalDateTime regDt;      // 등록일시
    private LocalDateTime updDt;      // 수정일시
}