package com.investment.portal.infrastructure.external.yahoo;

/**
 * Yahoo Finance 비공식 API에서 조회한 종목 정보
 */
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

    /** RAG 컨텍스트 삽입용 한 줄 요약 */
    public String toContextLine() {
        return String.format(
                "%s(%s) | 섹터: %s | 현재가: %.2f%s(%+.1f%%) | 시총: %s | PER: %.1f | 배당: %.2f%% | 52주: %.2f~%.2f",
                companyName, ticker, sector,
                currentPrice, currency, changePercent * 100,
                formatMarketCap(),
                peRatio, dividendYield * 100,
                fiftyTwoWeekLow, fiftyTwoWeekHigh
        );
    }

    private String formatMarketCap() {
        if (marketCap >= 1_000_000_000_000L) return String.format("%.1f조", marketCap / 1_000_000_000_000.0);
        if (marketCap >= 100_000_000_000L)   return String.format("%.0f억", marketCap / 100_000_000.0);
        return String.format("%,d", marketCap);
    }
}
