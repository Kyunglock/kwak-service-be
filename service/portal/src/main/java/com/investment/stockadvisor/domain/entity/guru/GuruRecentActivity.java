package com.investment.stockadvisor.domain.entity.guru;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuruRecentActivity {

    /** 고유 식별자 */
    private Long activityId;

    /** 투자자 고유 코드 */
    private String guruCd;

    /** 투자자 영문명 (예: Warren Buffett) */
    private String investorNm;

    /** 투자자 한글명 (예: 워런 버핏) */
    private String investorKoNm;

    /** 투자자 닉네임 */
    private String investorNickname;

    /** 매매 유형 (BUY / SELL / ADD / TRIM) */
    private String activityType;

    /** 거래 종목 회사명 */
    private String issuerNm;

    /** 종목 티커 심볼 (예: AAPL) */
    private String ticker;

    /** 매매 발생 분기 (예: Q1 2024) */
    private String activityDate;

    /** 매매 발생 연도 (예: 2025) */
    private Short activityYear;

    /** 매매 발생 분기 (1~4) */
    private Byte activityQtr;

    /** 포지션 변동 비율 (%) - 양수: 증가, 음수: 감소 */
    private BigDecimal changePct;

    /** 비고 및 추가 메모 */
    private String notes;

    /** 데이터 등록 일시 */
    private LocalDateTime regDt;
}
