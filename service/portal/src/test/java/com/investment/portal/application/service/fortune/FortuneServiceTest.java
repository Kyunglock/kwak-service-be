package com.investment.portal.application.service.fortune;

import com.investment.portal.application.dto.fortune.FortuneResponse;
import com.investment.portal.domain.entity.fortune.StockFortune;
import com.investment.portal.domain.repository.fortune.FortuneMapper;
import kwak.common.ai.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FortuneServiceTest {

    @Mock FortuneMapper fortuneMapper;
    @Mock AiGatewayClient aiGatewayClient;
    @InjectMocks FortuneServiceImpl service;

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private StockFortune saved(String ticker, String content) {
        return StockFortune.builder()
                .fortuneId(1L).ticker(ticker).fortuneDate(TODAY)
                .content(content).useYn("Y").regDt(LocalDateTime.now())
                .build();
    }

    // ---- 정규화 / 검증 ----

    @Test
    void 소문자_공백_입력은_대문자_트림으로_정규화되어_조회된다() {
        when(fortuneMapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(fortuneMapper.findByTickerAndDate("AAPL", TODAY)).thenReturn(Optional.of(saved("AAPL", "운세")));

        FortuneResponse res = service.getFortune("  aapl ");

        assertThat(res.ticker()).isEqualTo("AAPL");
        verify(fortuneMapper).findCanonicalTicker("AAPL");
    }

    @Test
    void 형식위반_입력은_매퍼_조회없이_UnsupportedTickerException() {
        for (String bad : new String[]{null, "", "애플", "AAPL!", "ABCDEFGHIJKLMNOPQRSTU"}) {
            assertThatThrownBy(() -> service.getFortune(bad))
                    .as(String.valueOf(bad))
                    .isInstanceOf(UnsupportedTickerException.class);
        }
        verifyNoInteractions(fortuneMapper, aiGatewayClient);
    }

    @Test
    void 미등록_티커는_UnsupportedTickerException() {
        when(fortuneMapper.findCanonicalTicker("ZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFortune("ZZZZ"))
                .isInstanceOf(UnsupportedTickerException.class);
        verifyNoInteractions(aiGatewayClient);
    }

    @Test
    void KR_종목코드는_정식형으로_변환되어_캐시키로_쓰인다() {
        when(fortuneMapper.findCanonicalTicker("005930")).thenReturn(Optional.of("005930.KS"));
        when(fortuneMapper.findByTickerAndDate("005930.KS", TODAY))
                .thenReturn(Optional.of(saved("005930.KS", "운세")));

        FortuneResponse res = service.getFortune("005930");

        assertThat(res.ticker()).isEqualTo("005930.KS");
    }

    // ---- 캐시 ----

    @Test
    void 오늘자_캐시_히트면_LLM을_호출하지_않는다() {
        when(fortuneMapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(fortuneMapper.findByTickerAndDate("AAPL", TODAY)).thenReturn(Optional.of(saved("AAPL", "캐시된 운세")));

        FortuneResponse res = service.getFortune("AAPL");

        assertThat(res.content()).isEqualTo("캐시된 운세");
        verifyNoInteractions(aiGatewayClient);
        verify(fortuneMapper, never()).insert(any());
    }

    @Test
    void 캐시_미스면_LLM_생성후_INSERT하고_재조회_결과를_반환한다() {
        when(fortuneMapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(fortuneMapper.findByTickerAndDate("AAPL", TODAY))
                .thenReturn(Optional.empty(), Optional.of(saved("AAPL", "새 운세")));
        when(aiGatewayClient.generateContent(anyString(), eq("종목: AAPL"))).thenReturn("새 운세");

        FortuneResponse res = service.getFortune("AAPL");

        assertThat(res.content()).isEqualTo("새 운세");
        verify(fortuneMapper).insert(argThat(f ->
                f.getTicker().equals("AAPL") && f.getFortuneDate().equals(TODAY) && f.getContent().equals("새 운세")));
    }

    @Test
    void 동시_경합으로_DuplicateKeyException이_나면_재SELECT_결과를_반환한다() {
        when(fortuneMapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(fortuneMapper.findByTickerAndDate("AAPL", TODAY))
                .thenReturn(Optional.empty(), Optional.of(saved("AAPL", "선점된 운세")));
        when(aiGatewayClient.generateContent(anyString(), anyString())).thenReturn("내가 만든 운세");
        doThrow(new DuplicateKeyException("dup")).when(fortuneMapper).insert(any());

        FortuneResponse res = service.getFortune("AAPL");

        assertThat(res.content()).isEqualTo("선점된 운세");
    }

    // ---- LLM 장애 ----

    @Test
    void LLM이_예외를_던지면_FortuneUnavailableException으로_변환하고_저장하지_않는다() {
        when(fortuneMapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(fortuneMapper.findByTickerAndDate("AAPL", TODAY)).thenReturn(Optional.empty());
        when(aiGatewayClient.generateContent(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.getFortune("AAPL"))
                .isInstanceOf(FortuneUnavailableException.class);
        verify(fortuneMapper, never()).insert(any());
    }

    @Test
    void LLM이_null이나_빈문자열을_반환하면_FortuneUnavailableException() {
        when(fortuneMapper.findCanonicalTicker("AAPL")).thenReturn(Optional.of("AAPL"));
        when(fortuneMapper.findByTickerAndDate("AAPL", TODAY)).thenReturn(Optional.empty());
        when(aiGatewayClient.generateContent(anyString(), anyString())).thenReturn(null, "", "null");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.getFortune("AAPL"))
                    .isInstanceOf(FortuneUnavailableException.class);
        }
        verify(fortuneMapper, never()).insert(any());
    }
}
