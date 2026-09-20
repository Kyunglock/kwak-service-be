package com.investment.portal.application.service.trade;

import com.investment.portal.application.dto.history.transaction.TransactionHistoryAddRequest;
import com.investment.portal.application.dto.history.transaction.TransactionHistoryResponse;
import com.investment.portal.application.dto.trade.*;
import com.investment.portal.application.service.history.TransactionHistoryService;
import com.investment.portal.domain.entity.portfolio.Portfolio;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import kwak.common.ai.AiGatewayClient;
import kwak.common.application.event.ActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeCaptureServiceImpl implements TradeCaptureService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 한국 종목 티커 형식 (005930.KS) — 통화 추정에 쓴다 */
    private static final Pattern KR_TICKER = Pattern.compile("^\\d{6}\\.(KS|KQ|KX)$");

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    /** 원본 이미지 상한. base64는 약 1.34배로 늘어나므로 게이트웨이 페이로드는 이보다 크다 */
    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;

    /** 거래일 하한 — 오타로 1900년대가 들어오는 것을 막는다 */
    private static final LocalDate MIN_TRADE_DATE = LocalDate.of(1990, 1, 1);

    private final PortfolioMapper portfolioMapper;
    private final AiGatewayClient aiGatewayClient;
    private final TradeExtractionParser parser;
    private final StockResolver stockResolver;
    private final TradeDraftStore draftStore;
    private final TransactionHistoryService transactionHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    // ── 초안 생성 ────────────────────────────────────────────────────────────────

    @Override
    public TradeDraftResponse captureText(String userId, TradeCaptureTextRequest request) {
        requireOwnedPortfolio(userId, request.portfolioId());

        String content = callLlm(
                () -> aiGatewayClient.chat(
                        TradeExtractionPrompt.SYSTEM,
                        TradeExtractionPrompt.TEXT_USER_PREFIX + request.text()));

        return buildDraft(userId, request.portfolioId(), parser.parse(content));
    }

    @Override
    public TradeDraftResponse captureImage(String userId, Long portfolioId, MultipartFile image) {
        requireOwnedPortfolio(userId, portfolioId);
        AiGatewayClient.ImagePart part = toImagePart(image);

        String content = callLlm(
                () -> aiGatewayClient.vision(
                        TradeExtractionPrompt.SYSTEM,
                        TradeExtractionPrompt.IMAGE_USER,
                        List.of(part)));

        return buildDraft(userId, portfolioId, parser.parse(content));
    }

    private String callLlm(java.util.function.Supplier<AiGatewayClient.ChatResponse> call) {
        try {
            AiGatewayClient.ChatResponse response = call.get();
            return response == null ? null : response.content();
        } catch (Exception e) {
            log.error("[TradeCapture] AI 추출 호출 실패", e);
            throw new TradeCaptureUnavailableException(e);
        }
    }

    private TradeDraftResponse buildDraft(String userId, Long portfolioId, List<ExtractedTrade> extracted) {
        String draftId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now(KST);

        List<TradeDraftItem> items = new ArrayList<>();
        int lineNo = 1;
        for (ExtractedTrade trade : extracted) {
            items.add(toDraftItem(lineNo++, trade, today));
        }

        int readyCount = (int) items.stream().filter(TradeDraftItem::ready).count();
        int needsReviewCount = items.size() - readyCount;

        // 초안은 저장 전 임시 상태이므로 Redis에만 둔다. 확정 시 소유자·포트폴리오를 여기서 다시 읽는다.
        draftStore.save(draftId, userId, portfolioId);

        log.info("[TradeCapture] 초안 생성 - userId: {}, portfolioId: {}, 추출 {}건 (확정가능 {}건)",
                userId, portfolioId, items.size(), readyCount);

        return new TradeDraftResponse(
                draftId, portfolioId, items, readyCount, needsReviewCount,
                notice(items.size(), readyCount, needsReviewCount));
    }

    private TradeDraftItem toDraftItem(int lineNo, ExtractedTrade trade, LocalDate today) {
        StockResolver.Resolution resolution = stockResolver.resolve(trade.name(), trade.ticker());

        List<TradeDraftItem.StockCandidate> candidates = resolution.candidates().stream()
                .map(ref -> new TradeDraftItem.StockCandidate(ref.stockCd(), ref.stockNm()))
                .toList();

        String stockCd = resolution.stockCd();
        BigDecimal qty = trade.qty();
        BigDecimal price = trade.price();
        LocalDate transDt = trade.date();
        String currency = trade.currency() != null ? trade.currency() : defaultCurrency(stockCd);

        String status;
        String issue;

        if (stockCd == null) {
            status = "NEEDS_STOCK";
            issue = candidates.isEmpty()
                    ? "'" + safeName(trade) + "' 종목을 찾지 못했습니다. 종목을 직접 선택해 주세요."
                    : "'" + safeName(trade) + "' 이(가) 여러 종목과 비슷합니다. 아래에서 골라 주세요.";
        } else if (qty == null || qty.signum() <= 0) {
            status = "NEEDS_INPUT";
            issue = "수량을 읽지 못했습니다. 직접 입력해 주세요.";
        } else if (price == null || price.signum() <= 0) {
            status = "NEEDS_INPUT";
            issue = "단가를 읽지 못했습니다. 직접 입력해 주세요.";
        } else if (transDt != null && transDt.isAfter(today)) {
            status = "NEEDS_INPUT";
            issue = "거래일이 미래(" + transDt + ")로 읽혔습니다. 확인해 주세요.";
        } else if (transDt == null) {
            // 날짜만 없는 경우는 오늘로 채우고 확정 가능하게 둔다 — 화면에서 바로 고칠 수 있다
            transDt = today;
            status = "READY";
            issue = "날짜를 찾지 못해 오늘(" + today + ")로 넣었습니다. 맞는지 확인해 주세요.";
        } else {
            status = "READY";
            issue = null;
        }

        return new TradeDraftItem(
                lineNo, safeName(trade), stockCd, resolution.stockNm(),
                trade.type(), transDt, qty, price, currency,
                status, issue, candidates);
    }

    private String safeName(ExtractedTrade trade) {
        if (trade.name() != null) {
            return trade.name();
        }
        return trade.ticker() != null ? trade.ticker() : "(종목 미상)";
    }

    private String defaultCurrency(String stockCd) {
        if (stockCd == null) {
            return null;
        }
        return KR_TICKER.matcher(stockCd).matches() ? "KRW" : "USD";
    }

    private String notice(int total, int ready, int needsReview) {
        if (total == 0) {
            return "매매 내역을 찾지 못했습니다. 종목과 수량, 단가를 포함해 다시 알려주세요.";
        }
        if (needsReview == 0) {
            return ready + "건을 찾았습니다. 내용을 확인하고 저장해 주세요.";
        }
        return total + "건 중 " + needsReview + "건은 확인이 필요합니다.";
    }

    // ── 확정 ─────────────────────────────────────────────────────────────────────

    @Override
    public TradeConfirmResponse confirm(String userId, TradeConfirmRequest request) {
        TradeDraftStore.Draft draft = draftStore.find(request.draftId())
                .orElseThrow(() -> new TradeDraftNotFoundException(request.draftId()));

        if (!draft.userId().equals(userId)) {
            // 남의 초안ID를 알아내더라도 그 포트폴리오에 쓸 수 없게 한다
            throw new TradeDraftNotFoundException(request.draftId());
        }

        // 대상 포트폴리오는 클라이언트 입력이 아니라 초안에 기록된 값을 쓴다
        Long portfolioId = draft.portfolioId();
        requireOwnedPortfolio(userId, portfolioId);

        LocalDate today = LocalDate.now(KST);
        List<TradeConfirmResponse.Result> results = new ArrayList<>();
        int saved = 0;

        for (TradeConfirmRequest.Item item : request.items()) {
            try {
                results.add(save(portfolioId, item, today));
                saved++;
            } catch (Exception e) {
                // 한 건이 실패해도 나머지는 저장한다 — 10건 중 1건 때문에 전부 다시 입력시키지 않는다
                log.warn("[TradeCapture] 거래 저장 실패 - line: {}, stockCd: {}, 사유: {}",
                        item.lineNo(), item.stockCd(), e.getMessage());
                results.add(new TradeConfirmResponse.Result(
                        item.lineNo(), item.stockCd(), false, null, message(e)));
            }
        }

        // 모두 실패했다면 초안을 남겨 사용자가 고쳐서 다시 시도할 수 있게 한다
        if (saved > 0) {
            draftStore.remove(request.draftId());
            eventPublisher.publishEvent(ActivityEvent.of(
                    userId, "TRADE_CAPTURE_CONFIRM", "TRANSACTION", null,
                    "AI 매매기록 " + saved + "건 저장"));
        }

        log.info("[TradeCapture] 확정 완료 - userId: {}, portfolioId: {}, 성공 {}건 / 실패 {}건",
                userId, portfolioId, saved, results.size() - saved);

        return new TradeConfirmResponse(saved, results.size() - saved, results);
    }

    /**
     * 확정 시점에 모든 값을 서버에서 다시 검증한다.
     * 초안 응답을 그대로 돌려받는 구조라 클라이언트가 보낸 값을 신뢰할 수 없다.
     */
    private TradeConfirmResponse.Result save(Long portfolioId, TradeConfirmRequest.Item item, LocalDate today) {
        String stockCd = stockResolver.resolve(null, item.stockCd()).stockCd();
        if (stockCd == null) {
            throw new IllegalArgumentException("등록되지 않은 종목입니다: " + item.stockCd());
        }

        String transType = "SELL".equalsIgnoreCase(item.transType()) ? "SELL" : "BUY";

        if (item.qty() == null || item.qty().signum() <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다");
        }
        if (item.price() == null || item.price().signum() <= 0) {
            throw new IllegalArgumentException("단가는 0보다 커야 합니다");
        }
        if (item.transDt().isAfter(today)) {
            throw new IllegalArgumentException("거래일은 미래일 수 없습니다: " + item.transDt());
        }
        if (item.transDt().isBefore(MIN_TRADE_DATE)) {
            throw new IllegalArgumentException("거래일이 올바르지 않습니다: " + item.transDt());
        }

        String currency = item.currency() != null ? item.currency() : defaultCurrency(stockCd);

        TransactionHistoryResponse response = transactionHistoryService.addTransaction(
                new TransactionHistoryAddRequest(
                        portfolioId, stockCd, transType, item.transDt(),
                        item.qty(), item.price(), null, null, currency,
                        item.memo() != null ? item.memo() : "AI 기록"));

        return new TradeConfirmResponse.Result(
                item.lineNo(), stockCd, true, response.transId(), null);
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────────

    /**
     * 포트폴리오 소유자 확인.
     *
     * <p>기존 PortfolioItemController 에는 이 검사가 없지만, AI가 쓰기 경로를 여는 이상
     * 여기서는 반드시 막는다.
     */
    private void requireOwnedPortfolio(String userId, Long portfolioId) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("포트폴리오ID는 필수입니다");
        }
        Portfolio portfolio = portfolioMapper.findByPortfolioId(portfolioId);
        if (portfolio == null || !userId.equals(portfolio.getUserId())) {
            throw new PortfolioAccessDeniedException(portfolioId);
        }
    }

    private AiGatewayClient.ImagePart toImagePart(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 비어 있습니다");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("이미지는 4MB 이하만 올릴 수 있습니다");
        }
        String contentType = image.getContentType() == null
                ? "" : image.getContentType().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다: " + contentType);
        }
        try {
            return new AiGatewayClient.ImagePart(
                    contentType, Base64.getEncoder().encodeToString(image.getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException("이미지를 읽지 못했습니다", e);
        }
    }

    private String message(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? "저장에 실패했습니다" : m;
    }
}
