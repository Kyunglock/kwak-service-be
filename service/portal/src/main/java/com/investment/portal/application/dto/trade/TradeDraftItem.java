package com.investment.portal.application.dto.trade;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "AI가 추출한 매매 기록 초안 한 줄. 사용자가 확인·수정한 뒤에야 저장된다.")
public record TradeDraftItem(

        @Schema(description = "초안 내 줄 번호", example = "1")
        int lineNo,

        @Schema(description = "원문에 쓰인 종목 표기 (사용자가 무엇을 보고 이렇게 해석했는지 보여주기 위함)", example = "애플")
        String rawName,

        @Schema(description = "DB에서 검증된 정식 티커. 해석 실패 시 null", example = "AAPL")
        String stockCd,

        @Schema(description = "DB 종목명", example = "Apple Inc.")
        String stockNm,

        @Schema(description = "거래유형", example = "BUY", allowableValues = {"BUY", "SELL"})
        String transType,

        @Schema(description = "거래일자", example = "2026-09-18")
        LocalDate transDt,

        @Schema(description = "수량", example = "10")
        BigDecimal qty,

        @Schema(description = "단가", example = "230.15")
        BigDecimal price,

        @Schema(description = "통화", example = "USD")
        String currency,

        @Schema(description = "상태", example = "READY",
                allowableValues = {"READY", "NEEDS_STOCK", "NEEDS_INPUT"})
        String status,

        @Schema(description = "사용자에게 보여줄 확인 요청 사유. 없으면 null", example = "날짜가 없어 오늘로 넣었습니다")
        String issue,

        @Schema(description = "종목 해석 실패 시 제시하는 후보 목록")
        List<StockCandidate> candidates
) {
    /** 저장 가능한 상태인지. */
    public boolean ready() {
        return "READY".equals(status);
    }

    @Schema(description = "종목 후보")
    public record StockCandidate(
            @Schema(description = "티커", example = "AAPL") String stockCd,
            @Schema(description = "종목명", example = "Apple Inc.") String stockNm) {}
}
