package com.investment.portal.domain.entity.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    
    private Long portfolioId;         // 포트폴리오ID
    private String userId;            // 사용자ID
    private String portfolioNm;       // 포트폴리오명
    private String portfolioDesc;     // 포트폴리오 설명
    private String baseCurrency;      // 기준통화
    private String useYn;             // 사용여부
    private LocalDateTime regDt;      // 등록일시
    private LocalDateTime updDt;      // 수정일시
}