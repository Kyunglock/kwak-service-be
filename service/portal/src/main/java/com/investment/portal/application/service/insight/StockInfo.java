package com.investment.portal.application.service.insight;

import org.apache.ibatis.type.Alias;

/**
 * 인사이트 생성에 사용하는 종목 시장 정보.
 * DB(tbl_companies + tbl_stock_price_history + tbl_dividend_history)에서 조립된다.
 * peRatio/marketCap/industry는 현재 DB에 없어 0/""로 채워지며,
 * peRatio는 소비 측에서 "미집계"(peRatio>0 필터)로 처리된다.
 */
@Alias("InsightStockInfo")
public record StockInfo(
        String ticker,
        String companyName,
        String sector,
        String industry,
        double currentPrice,
        double changePercent,       // 당일 등락률 (소수: 0.015 = +1.5%)
        long   marketCap,
        double peRatio,             // 후행 PER (0이면 미집계)
        double dividendYield,       // 배당수익률 (소수: 0.02 = 2%)
        double fiftyTwoWeekLow,
        double fiftyTwoWeekHigh,
        String currency
) {
    /** 52주 범위에서 현재가 위치 (0~100%) */
    public double pricePosition() {
        double range = fiftyTwoWeekHigh - fiftyTwoWeekLow;
        if (range <= 0) return 50.0;
        return (currentPrice - fiftyTwoWeekLow) / range * 100.0;
    }

    /** RAG 컨텍스트 삽입용 한 줄 요약. DB에 없는 값(PER 0, 시총 0)은 생략한다. */
    public String toContextLine() {
        StringBuilder sb = new StringBuilder(String.format(
                "%s(%s) | 섹터: %s | 현재가: %.2f%s(%+.1f%%)",
                companyName, ticker, sector,
                currentPrice, currency, changePercent * 100));
        if (marketCap > 0) sb.append(" | 시총: ").append(formatMarketCap());
        if (peRatio > 0)   sb.append(String.format(" | PER: %.1f", peRatio));
        sb.append(String.format(" | 배당: %.2f%% | 52주: %.2f~%.2f",
                dividendYield * 100, fiftyTwoWeekLow, fiftyTwoWeekHigh));
        return sb.toString();
    }

    private String formatMarketCap() {
        if (marketCap >= 1_000_000_000_000L) return String.format("%.1f조", marketCap / 1_000_000_000_000.0);
        if (marketCap >= 100_000_000_000L)   return String.format("%.0f억", marketCap / 100_000_000.0);
        return String.format("%,d", marketCap);
    }
}
