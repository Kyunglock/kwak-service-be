package com.investment.portal.api.controller.trade;

import com.investment.portal.application.dto.trade.*;
import com.investment.portal.application.service.trade.PortfolioAccessDeniedException;
import com.investment.portal.application.service.trade.TradeCaptureService;
import com.investment.portal.application.service.trade.TradeCaptureUnavailableException;
import com.investment.portal.application.service.trade.TradeDraftNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI 매매기록", description = "문장·화면 캡처에서 매매 내역을 추출해 포트폴리오에 기록하는 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/trades/capture")
@RequiredArgsConstructor
public class TradeCaptureController {

    private final TradeCaptureService tradeCaptureService;

    @Operation(
            summary = "문장에서 매매 내역 추출 (저장 안 함)",
            description = """
                    "어제 애플 10주 230달러에 샀어" 같은 문장에서 매매 내역을 뽑아 초안을 돌려줍니다.
                    이 단계에서는 아무것도 저장되지 않습니다. 확정하려면 /confirm 을 호출하세요.
                    """)
    @PostMapping("/text")
    public ResponseEntity<?> captureText(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody TradeCaptureTextRequest request) {

        TradeDraftResponse draft = tradeCaptureService.captureText(userId, request);
        return ResponseUtil.success(draft, draft.notice());
    }

    @Operation(
            summary = "화면 캡처에서 매매 내역 추출 (저장 안 함)",
            description = """
                    증권사 앱 스크린샷에서 매매 내역을 뽑아 초안을 돌려줍니다.
                    png/jpeg/webp/gif, 4MB 이하만 지원합니다.
                    """)
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> captureImage(
            @AuthenticationPrincipal String userId,
            @Parameter(description = "포트폴리오ID", example = "10")
            @RequestParam Long portfolioId,
            @Parameter(description = "증권사 화면 캡처 이미지")
            @RequestPart("image") MultipartFile image) {

        TradeDraftResponse draft = tradeCaptureService.captureImage(userId, portfolioId, image);
        return ResponseUtil.success(draft, draft.notice());
    }

    @Operation(
            summary = "초안 확정 — 거래내역으로 저장",
            description = """
                    사용자가 확인·수정한 항목을 실제 거래내역으로 저장하고 보유종목에 반영합니다.
                    저장 대상 포트폴리오는 초안에 기록된 값을 사용하므로 요청으로 바꿀 수 없습니다.
                    건별로 성공/실패가 나뉠 수 있습니다.
                    """)
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody TradeConfirmRequest request) {

        TradeConfirmResponse response = tradeCaptureService.confirm(userId, request);
        String message = response.failedCount() == 0
                ? response.savedCount() + "건을 저장했습니다"
                : response.savedCount() + "건 저장, " + response.failedCount() + "건 실패";
        return ResponseUtil.success(response, message);
    }

    // ── 예외 처리 ────────────────────────────────────────────────────────────────
    // GlobalExceptionHandler 의 catch-all 은 전부 500으로 내보내므로,
    // 사용자가 스스로 고칠 수 있는 실패는 여기서 상태코드와 문구를 정확히 잡아준다.

    @ExceptionHandler(PortfolioAccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(PortfolioAccessDeniedException e) {
        return ResponseUtil.error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(TradeDraftNotFoundException.class)
    public ResponseEntity<?> handleDraftNotFound(TradeDraftNotFoundException e) {
        return ResponseUtil.notFound(e.getMessage());
    }

    @ExceptionHandler(TradeCaptureUnavailableException.class)
    public ResponseEntity<?> handleUnavailable(TradeCaptureUnavailableException e) {
        return ResponseUtil.error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseUtil.badRequest(e.getMessage());
    }
}
