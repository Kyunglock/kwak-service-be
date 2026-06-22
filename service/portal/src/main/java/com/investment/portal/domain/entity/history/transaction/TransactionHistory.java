package com.investment.portal.domain.entity.history.transaction;

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
public class TransactionHistory {
    
    private Long transId;             // 거래ID
    private Long portfolioId;         // 포트폴리오ID
    private String stockCd;           // 종목코드
    private String transType;         // 거래유형 (BUY, SELL)
    private LocalDate transDt;        // 거래일자
    private BigDecimal qty;           // 수량
    private BigDecimal price;         // 단가
    private BigDecimal amount;        // 거래금액
    private BigDecimal fee;           // 수수료
    private BigDecimal tax;           // 세금
    private String currency;          // 통화
    private String memo;              // 메모
    private LocalDateTime regDt;      // 등록일시
}