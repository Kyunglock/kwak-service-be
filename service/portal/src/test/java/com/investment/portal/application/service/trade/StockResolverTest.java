package com.investment.portal.application.service.trade;

import com.investment.portal.domain.repository.stock.StockRef;
import com.investment.portal.domain.repository.stock.StockResolveMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockResolverTest {

    @Mock StockResolveMapper mapper;
    @InjectMocks StockResolver resolver;

    @Test
    void LLM이_준_티커도_DB에_있어야만_확정된다() {
        when(mapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(mapper.findStockNameByTicker("AAPL")).thenReturn(Optional.of("Apple Inc."));

        StockResolver.Resolution res = resolver.resolve("애플", "AAPL");

        assertThat(res.isResolved()).isTrue();
        assertThat(res.stockCd()).isEqualTo("AAPL");
        assertThat(res.stockNm()).isEqualTo("Apple Inc.");
    }

    @Test
    void DB에_없는_티커를_지어내면_확정되지_않는다() {
        // 존재하지 않는 티커. 종목명 조회도 모두 빈 결과
        when(mapper.findCanonicalTicker(anyString())).thenReturn(Optional.empty());
        when(mapper.findByExactName("애플전자")).thenReturn(List.of());
        when(mapper.findByPartialName(eq("애플전자"), anyInt())).thenReturn(List.of());

        StockResolver.Resolution res = resolver.resolve("애플전자", "APLE");

        assertThat(res.isResolved()).isFalse();
        assertThat(res.stockCd()).isNull();
    }

    @Test
    void 종목명_자리에_티커가_와도_해석된다() {
        when(mapper.findCanonicalTicker("TSLA")).thenReturn(Optional.of("TSLA"));
        when(mapper.findStockNameByTicker("TSLA")).thenReturn(Optional.of("Tesla, Inc."));

        StockResolver.Resolution res = resolver.resolve("tsla", null);

        assertThat(res.stockCd()).isEqualTo("TSLA");
    }

    @Test
    void 종목명_정확일치가_하나면_확정된다() {
        when(mapper.findCanonicalTicker(anyString())).thenReturn(Optional.empty());
        when(mapper.findByExactName("애플")).thenReturn(List.of(new StockRef("AAPL", "Apple Inc.")));

        StockResolver.Resolution res = resolver.resolve("애플", null);

        assertThat(res.stockCd()).isEqualTo("AAPL");
        assertThat(res.stockNm()).isEqualTo("Apple Inc.");
    }

    @Test
    void 동명이의면_확정하지_않고_후보를_돌려준다() {
        when(mapper.findCanonicalTicker(anyString())).thenReturn(Optional.empty());
        when(mapper.findByExactName("한국전력")).thenReturn(List.of(
                new StockRef("015760.KS", "한국전력"),
                new StockRef("KEP", "Korea Electric Power")));

        StockResolver.Resolution res = resolver.resolve("한국전력", null);

        assertThat(res.isResolved()).isFalse();
        assertThat(res.candidates()).hasSize(2);
        verify(mapper, never()).findByPartialName(anyString(), anyInt());
    }

    @Test
    void 부분일치는_자동선택하지_않고_후보만_제시한다() {
        when(mapper.findCanonicalTicker(anyString())).thenReturn(Optional.empty());
        when(mapper.findByExactName("삼성")).thenReturn(List.of());
        when(mapper.findByPartialName(eq("삼성"), anyInt())).thenReturn(List.of(
                new StockRef("005930.KS", "삼성전자"),
                new StockRef("006400.KS", "삼성SDI")));

        StockResolver.Resolution res = resolver.resolve("삼성", null);

        assertThat(res.isResolved()).isFalse();
        assertThat(res.candidates()).extracting(StockRef::stockCd)
                .containsExactly("005930.KS", "006400.KS");
    }

    @Test
    void 소문자_공백_입력은_정규화해서_조회한다() {
        when(mapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(mapper.findStockNameByTicker("AAPL")).thenReturn(Optional.of("Apple Inc."));

        assertThat(resolver.resolve("  aapl  ", null).stockCd()).isEqualTo("AAPL");
    }

    @Test
    void 종목_단서가_전혀_없으면_DB를_조회하지_않는다() {
        StockResolver.Resolution res = resolver.resolve(null, null);

        assertThat(res.isResolved()).isFalse();
        assertThat(res.candidates()).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void 한국종목은_suffix_정식형으로_반환된다() {
        when(mapper.findCanonicalTicker("005930")).thenReturn(Optional.of("005930.KS"));
        when(mapper.findStockNameByTicker("005930.KS")).thenReturn(Optional.of("삼성전자"));

        StockResolver.Resolution res = resolver.resolve(null, "005930");

        assertThat(res.stockCd()).isEqualTo("005930.KS");
    }
}
