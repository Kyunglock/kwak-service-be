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
public class GuruPortfolio {

    /** 고유 식별자 */
    private Long id;

    /** tbl_guru 참조 식별자 */
    private Long guruId;

    /** 투자자 영문명 (예: Warren Buffett) */
    private String investorNm;

    /** 투자자 한글명 (예: 워런 버핏) */
    private String investorKoNm;

    /** 투자자 닉네임 */
    private String investorNickname;

    /** 투자 철학 */
    private String investPhilosophy;

    /** 투자 대가의 명언 */
    private String famousQuote;

    /** 보유 종목 회사명 */
    private String issuerNm;

    /** 종목 티커 심볼 (예: AAPL) */
    private String ticker;

    /** 해당 투자자 포트폴리오 내 보유 순위 */
    private Integer rankNo;

    /** 포트폴리오 내 비중 (%) */
    private BigDecimal portfolioPct;

    /** 보고 기준일 (예: Q1 2024) */
    private String reportDate;

    /** 최근 매매 활동 일자 */
    private String activityDate;

    /** 매매 활동 연도 */
    private Integer activityYear;

    /** 매매 활동 분기 */
    private Integer activityQtr;

    /** 보유 비중 변동률 (%) */
    private BigDecimal changePct;

    /** 데이터 등록 일시 */
    private LocalDateTime regDt;
}
