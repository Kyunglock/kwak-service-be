package com.investment.portal.api.controller.qa;

import com.investment.portal.application.dto.qa.MarketQuestionRequest;
import com.investment.portal.application.dto.qa.MarketQuestionResponse;
import com.investment.portal.application.service.qa.MarketQuestionService;
import com.investment.portal.application.service.qa.MarketQuestionUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 시황 질의응답", description = "고정 질문 유형(최대 하락/상승일, 기간 수익률, 최고/최저가, 배당)에 DB 근거로 답하는 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class MarketQuestionController {

    private final MarketQuestionService marketQuestionService;

    @Operation(
            summary = "시황 질문에 답변",
            description = """
                    "올해 애플 가장 많이 하락했던 날 무슨 일이 있었는지" 같은 질문에 DB 주가·배당
                    이력과 뉴스를 근거로 자연어로 답합니다. 지원하지 않는 질문이거나 종목을 특정하지
                    못하면 200 응답 안에서 안내 문구로 답합니다(에러가 아님) — 채팅 UI가 일반 대화처럼
                    렌더할 수 있게 하기 위함입니다.
                    """)
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@Valid @RequestBody MarketQuestionRequest request) {
        MarketQuestionResponse response = marketQuestionService.ask(request);
        return ResponseUtil.success(response, "답변 생성 완료");
    }

    // ── 예외 처리 ────────────────────────────────────────────────────────────────
    // 질문 미이해/종목 미확정/데이터 없음은 서비스가 200 응답의 answer 문구로 흡수한다.
    // 여기서 다루는 건 AI 모듈 호출 자체가 실패한 진짜 장애뿐이다.

    @ExceptionHandler(MarketQuestionUnavailableException.class)
    public ResponseEntity<?> handleUnavailable(MarketQuestionUnavailableException e) {
        return ResponseUtil.error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseUtil.badRequest(e.getMessage());
    }
}
