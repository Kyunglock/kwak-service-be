package com.investment.portal.insight;

import com.investment.portal.application.dto.stock.StockContextRow;
import com.investment.portal.application.dto.stock.StockDividendRow;
import com.investment.portal.application.service.insight.PortfolioStockInfoProvider;
import com.investment.portal.application.service.insight.StockInfo;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import com.investment.portal.domain.repository.dividend.DividendHistoryMapper;
import com.investment.portal.domain.repository.stock.StockPriceHistoryMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioStockInfoProviderTest {

    private PortfolioItem item(String stockCd, String currency) {
        return PortfolioItem.builder().stockCd(stockCd).currency(currency).build();
    }

    private StockContextRow row(String cd, String name, String sector,
                                String open, String close, String high, String low) {
        return StockContextRow.builder()
                .stockCd(cd).companyName(name).sector(sector)
                .openPrice(bd(open)).closePrice(bd(close))
                .week52High(bd(high)).week52Low(bd(low))
                .build();
    }

    private BigDecimal bd(String v) { return v == null ? null : new BigDecimal(v); }

    @Test
    void emptyItemsReturnsEmptyMap() {
        var provider = new PortfolioStockInfoProvider(
                mock(StockPriceHistoryMapper.class), mock(DividendHistoryMapper.class));
        assertThat(provider.fetchForItems(List.of())).isEmpty();
    }

    @Test
    void assemblesStockInfoWithYieldChangeAndPosition() {
        var priceMapper = mock(StockPriceHistoryMapper.class);
        var divMapper   = mock(DividendHistoryMapper.class);

        when(priceMapper.findStockContextByStockCodes(anyList())).thenReturn(List.of(
                // 현재가 110, 시가 100 → 등락 +10%, 52주 50~150 → 위치 (110-50)/100 = 60%
                row("GOOGL", "Alphabet", "Communication Services", "100", "110", "150", "50")));
        when(divMapper.findTrailingDividendByStockCodes(anyList())).thenReturn(List.of(
                // 배당합 2.2, 현재가 110 → 수익률 2%
                StockDividendRow.builder().stockCd("GOOGL").trailingDividend(bd("2.2")).build()));

        var provider = new PortfolioStockInfoProvider(priceMapper, divMapper);
        Map<String, StockInfo> map = provider.fetchForItems(List.of(item("GOOGL", "USD")));

        assertThat(map).containsKey("GOOGL");
        StockInfo s = map.get("GOOGL");
        assertThat(s.currentPrice()).isEqualTo(110.0);
        assertThat(s.changePercent()).isCloseTo(0.10, within(1e-9));
        assertThat(s.dividendYield()).isCloseTo(0.02, within(1e-9));
        assertThat(s.pricePosition()).isCloseTo(60.0, within(1e-9));
        assertThat(s.currency()).isEqualTo("USD");
        assertThat(s.sector()).isEqualTo("Communication Services");
        assertThat(s.peRatio()).isZero();       // DB 미보유 → 미집계
        assertThat(s.marketCap()).isZero();
    }

    @Test
    void excludesTickersWithoutPrice() {
        var priceMapper = mock(StockPriceHistoryMapper.class);
        var divMapper   = mock(DividendHistoryMapper.class);

        when(priceMapper.findStockContextByStockCodes(anyList())).thenReturn(List.of(
                row("AAA", "AAA Corp", "Tech", "10", "12", "15", "8"),
                row("BBB", "BBB Corp", "Tech", null, null, null, null)));   // 가격 없음
        when(divMapper.findTrailingDividendByStockCodes(anyList())).thenReturn(List.of());

        var provider = new PortfolioStockInfoProvider(priceMapper, divMapper);
        Map<String, StockInfo> map = provider.fetchForItems(
                List.of(item("AAA", "USD"), item("BBB", "USD")));

        assertThat(map).containsOnlyKeys("AAA");
        assertThat(map.get("AAA").dividendYield()).isZero();   // 배당 데이터 없음
    }
}
