package com.investment.portal.api.controller.fortune;

import com.investment.portal.application.service.fortune.FortuneService;
import com.investment.portal.application.service.fortune.FortuneUnavailableException;
import com.investment.portal.application.service.fortune.UnsupportedTickerException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "종목운세", description = "종목·일자당 1건 전역 공유되는 재미용 운세 API")
@RestController
@RequestMapping("/api/v1/fortunes")
@RequiredArgsConstructor
public class FortuneController {

    private final FortuneService fortuneService;

    @Operation(
        summary = "종목운세 조회",
        description = "(정식 티커, KST 오늘)당 1건 전역 캐시. 미생성 시 로컬 LLM으로 동기 생성하므로 수십 초 걸릴 수 있습니다."
    )
    @GetMapping("/{ticker}")
    public ResponseEntity<?> getFortune(
            @Parameter(description = "티커 (US: AAPL, KR: 005930 또는 005930.KS)", example = "AAPL")
            @PathVariable String ticker) {
        try {
            return ResponseUtil.success(fortuneService.getFortune(ticker), "종목운세 조회 성공");
        } catch (UnsupportedTickerException e) {
            // errorCode 필수 — FE 인터셉터가 errorCode 없는 404를 /error 페이지로 리다이렉트함
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "errorCode", "TICKER_NOT_FOUND", "message", e.getMessage()));
        } catch (FortuneUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "errorCode", "AI_UNAVAILABLE", "message", e.getMessage()));
        }
    }
}
