package com.investment.portal.api.controller.fortune;

import com.investment.portal.application.dto.fortune.FortuneResponse;
import com.investment.portal.application.service.fortune.FortuneService;
import com.investment.portal.application.service.fortune.FortuneUnavailableException;
import com.investment.portal.application.service.fortune.UnsupportedTickerException;
import kwak.common.application.dto.RokResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FortuneControllerTest {

    @Mock FortuneService fortuneService;
    @InjectMocks FortuneController controller;

    @Test
    void 정상조회는_200과_운세를_반환한다() {
        FortuneResponse fortune = new FortuneResponse("AAPL", LocalDate.of(2026, 7, 13), "운세", LocalDateTime.now());
        when(fortuneService.getFortune("AAPL")).thenReturn(fortune);

        ResponseEntity<?> res = controller.getFortune("AAPL");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        RokResponse<?> body = (RokResponse<?>) res.getBody();
        assertThat(body.isSuccess()).isTrue();
        assertThat(body.getData()).isEqualTo(fortune);
    }

    @Test
    void 미지원_티커는_404와_TICKER_NOT_FOUND_errorCode를_반환한다() {
        when(fortuneService.getFortune("ZZZZ")).thenThrow(new UnsupportedTickerException());

        ResponseEntity<?> res = controller.getFortune("ZZZZ");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // errorCode 필수 — FE 인터셉터가 errorCode 없는 404를 /error로 리다이렉트함
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertThat(body.get("errorCode")).isEqualTo("TICKER_NOT_FOUND");
        assertThat(body.get("message")).isEqualTo("지원하지 않는 종목입니다.");
    }

    @Test
    void LLM_장애는_503과_AI_UNAVAILABLE_errorCode를_반환한다() {
        when(fortuneService.getFortune("AAPL")).thenThrow(new FortuneUnavailableException());

        ResponseEntity<?> res = controller.getFortune("AAPL");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertThat(body.get("errorCode")).isEqualTo("AI_UNAVAILABLE");
        assertThat(body.get("message")).isEqualTo("점술가가 자리를 비웠습니다. 잠시 후 다시 시도해주세요.");
    }
}
