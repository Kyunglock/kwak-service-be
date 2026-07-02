package com.investment.analyzer.market_analyzer.domain.entity.stock;

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
public class StockInfo {
    
    private String stockCd;           // 종목코드 (예: AAPL, 005930)
    private String stockNm;           // 종목명
    private String stockNmEn;         // 종목명(영문)
    private String marketType;        // 시장구분 (US, KR, etc)
    private String exchange;          // 거래소 (NASDAQ, NYSE, KOSPI, KOSDAQ)
    private String sector;            // 섹터 (Technology, Healthcare, etc)
    private String industry;          // 산업군 (보다 세부적인 분류)
    private LocalDate listingDt;      // 상장일
    private String country;           // 국가
    private String useYn;             // 사용여부
    private LocalDateTime regDt;      // 등록일시
    private String regId;             // 등록자ID
    private LocalDateTime updDt;      // 수정일시
    private String updId;             // 수정자ID
}