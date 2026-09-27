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
import java.math.RoundingMode;
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

    /**
     * 수량 × 단가 와 총액의 허용 오차(0.5%).
     *
     * <p>둘 다 같은 화면에서 읽은 값이라 제대로 읽었다면 반올림 수준으로만 어긋난다.
     * 잡아내야 하는 건 매입가 자리에 현재가를 집어넣은 경우인데, 평가손익이 작은 종목은
     * 그 차이가 1%도 안 된다 — 실제 캡처에서 매입가 343.0188 과 현재가 339.73 은 0.96% 차이다.
     * 수수료가 포함된 매입금액(0.2%대)을 잘못 잡지 않는 선에서 최대한 좁혔다.
     * 헛짚어도 확인 한 번 더 받는 것으로 끝나지만, 놓치면 틀린 단가가 그대로 저장된다.
     */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.005");

    static final String PROMPT_ACTION_TYPE = "AI_TRADE_CAPTURE";
    static final String IMAGE_PROMPT_DETAIL = "[이미지 첨부]";

    /** 입력 경로. 날짜가 비었을 때 어떻게 다룰지가 갈린다. */
    private enum Source { TEXT, IMAGE }

    private final PortfolioMapper portfolioMapper;
    private final TradeExtractionGateway extractionGateway;
    private final TradeExtractionParser parser;
    private final StockResolver stockResolver;
    private final TradeDraftStore draftStore;
    private final TransactionHistoryService transactionHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    // ── 초안 생성 ────────────────────────────────────────────────────────────────

    @Override
    public TradeDraftResponse captureText(String userId, TradeCaptureTextRequest request) {
        requireOwnedPortfolio(userId, request.portfolioId());
        recordPrompt(userId, request.portfolioId(), request.text());

        // 추출 경로(n8n / ai 모듈 직접)는 게이트웨이가 고른다
        String content = extractionGateway.extractFromText(request.text());

        return buildDraft(userId, request.portfolioId(), parser.parse(content), Source.TEXT);
    }

    @Override
    public TradeDraftResponse captureImage(String userId, Long portfolioId, MultipartFile image) {
        requireOwnedPortfolio(userId, portfolioId);
        recordPrompt(userId, portfolioId, IMAGE_PROMPT_DETAIL);

        String content = extractionGateway.extractFromImage(toImagePart(image));

        return buildDraft(userId, portfolioId, parser.parse(content), Source.IMAGE);
    }

    /** 추출 전에 남긴다 — AI 호출이 실패한 입력도 기록에 있어야 원인을 되짚을 수 있다. */
    private void recordPrompt(String userId, Long portfolioId, String detail) {
        eventPublisher.publishEvent(ActivityEvent.of(
                userId, PROMPT_ACTION_TYPE, "PORTFOLIO", String.valueOf(portfolioId), detail));
    }

    private TradeDraftResponse buildDraft(
            String userId, Long portfolioId, TradeExtraction extraction, Source source) {

        String draftId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now(KST);

        List<TradeDraftItem> items = new ArrayList<>();
        int lineNo = 1;
        for (ExtractedTrade trade : extraction.trades()) {
            items.add(toDraftItem(lineNo++, trade, today, source));
        }

        int readyCount = (int) items.stream().filter(TradeDraftItem::ready).count();
        int needsReviewCount = items.size() - readyCount;

        // 초안은 저장 전 임시 상태이므로 Redis에만 둔다. 확정 시 소유자·포트폴리오를 여기서 다시 읽는다.
        draftStore.save(draftId, userId, portfolioId);

        log.info("[TradeCapture] 초안 생성 - userId: {}, portfolioId: {}, 경로: {}, 화면: {}, 추출 {}건 (확정가능 {}건)",
                userId, portfolioId, source, extraction.screenType(), items.size(), readyCount);

        return new TradeDraftResponse(
                draftId, portfolioId, items, readyCount, needsReviewCount,
                notice(extraction, items.size(), readyCount, needsReviewCount));
    }

    private TradeDraftItem toDraftItem(int lineNo, ExtractedTrade trade, LocalDate today, Source source) {
        StockResolver.Resolution resolution = stockResolver.resolve(trade.name(), trade.ticker());

        List<TradeDraftItem.StockCandidate> candidates = resolution.candidates().stream()
                .map(ref -> new TradeDraftItem.StockCandidate(ref.stockCd(), ref.stockNm()))
                .toList();

        String stockCd = resolution.stockCd();
        BigDecimal qty = trade.qty();
        BigDecimal price = trade.price();
        String currency = resolveCurrency(stockCd, trade.currency());
        LocalDate transDt = resolveDate(trade.date(), today, source);

        String status;
        String issue;

        // 남기는 확인 요청은 "서버가 대신 채울 수 없는 것"뿐이다.
        // 종목·수량·단가를 추측해 채우면 사용자 기록이 조용히 틀어진다.
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
        } else if (amountMismatch(qty, price, trade.amount())) {
            // 잔고 화면은 [매입금액 / 매입가] 와 [평가금액 / 현재가] 가 위아래로 붙어 있어
            // 모델이 두 쌍을 섞어 읽는다. 어느 쪽을 잘못 집었는지는 단정할 수 없으니
            // 고쳐 넣지 않고, 총액으로 역산한 값을 후보로 보여주고 사용자가 고르게 한다.
            status = "NEEDS_INPUT";
            issue = "단가와 금액이 맞지 않습니다 ("
                    + num(qty) + " × " + num(price) + " ≠ " + num(trade.amount()) + ")."
                    + " 금액 기준으로는 " + num(trade.amount().divide(qty, 4, RoundingMode.HALF_UP))
                    + " 입니다. 맞는 값을 확인해 주세요.";
        } else if (transDt == null) {
            status = "NEEDS_DATE";
            issue = "화면에 매입일이 없습니다. 날짜를 입력해 주세요.";
        } else {
            status = "READY";
            issue = null;
        }

        return new TradeDraftItem(
                lineNo, safeName(trade), stockCd, resolution.stockNm(),
                trade.type(), transDt, qty, price, currency,
                status, issue, candidates);
    }

    /**
     * 거래일 결정.
     *
     * <p>문장 입력은 날짜로 사용자를 붙잡지 않는다. 대부분 "애플 10주 샀어" 정도로만 말하고,
     * 날짜까지 또박또박 적어주길 기대하는 편이 비현실적이다. 없으면 오늘로 둔다.
     *
     * <p>화면 캡처는 다르다. 잔고 화면에는 매입일이 아예 없어서, 오늘로 채우면 몇 달 전에 산
     * 종목이 전부 오늘 매수로 기록된다. 보유기간과 수익률이 통째로 틀어지므로 반드시 묻는다.
     *
     * <p>미래 날짜는 어느 경로든 오독이다 — 오늘로 맞춘다.
     */
    private LocalDate resolveDate(LocalDate extracted, LocalDate today, Source source) {
        if (extracted == null) {
            return source == Source.IMAGE ? null : today;
        }
        return extracted.isAfter(today) ? today : extracted;
    }

    /**
     * 통화 결정.
     *
     * <p>해외잔고 화면은 상단 합계가 원화, 표는 달러인데 표에는 통화기호가 없다. 모델이 위쪽
     * '원'에 이끌려 KRW를 적어 올리면 343달러짜리가 343원이 된다. 티커가 확정된 뒤에는
     * 종목 자체가 통화를 말해주므로, 모델 값보다 티커를 믿는다.
     */
    private String resolveCurrency(String stockCd, String extracted) {
        if (stockCd == null) {
            return extracted; // 종목을 모르면 판단 근거가 없다
        }
        if (KR_TICKER.matcher(stockCd).matches()) {
            return "KRW";
        }
        if (extracted == null || "KRW".equals(extracted)) {
            // 접미사 없는 티커는 미국 상장이다. 그 외 시장은 함부로 단정하지 않는다.
            return stockCd.contains(".") ? extracted : "USD";
        }
        return extracted;
    }

    /** 수량 × 단가 가 총액과 어긋나는지. 셋 중 하나라도 없으면 검산하지 않는다. */
    private boolean amountMismatch(BigDecimal qty, BigDecimal price, BigDecimal amount) {
        if (qty == null || price == null || amount == null) {
            return false;
        }
        if (qty.signum() <= 0 || price.signum() <= 0 || amount.signum() <= 0) {
            return false;
        }
        BigDecimal diff = qty.multiply(price).subtract(amount).abs();
        return diff.compareTo(amount.multiply(AMOUNT_TOLERANCE)) > 0;
    }

    private String num(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String safeName(ExtractedTrade trade) {
        if (trade.name() != null) {
            return trade.name();
        }
        return trade.ticker() != null ? trade.ticker() : "(종목 미상)";
    }

    private String notice(TradeExtraction extraction, int total, int ready, int needsReview) {
        if (total == 0) {
            return "매매 내역을 찾지 못했습니다. 종목과 수량, 단가를 포함해 다시 알려주세요.";
        }
        // 잔고 화면은 "지금 들고 있는 것"이지 "오늘 산 것"이 아니다. 무엇으로 읽었는지 밝혀
        // 사용자가 매입일을 채우기 전에 저장해 버리는 일을 막는다.
        String prefix = extraction.isBalanceScreen() ? "잔고 화면으로 읽었습니다. " : "";
        if (needsReview == 0) {
            return prefix + ready + "건을 찾았습니다. 내용을 확인하고 저장해 주세요.";
        }
        return prefix + total + "건 중 " + needsReview + "건은 확인이 필요합니다.";
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

        String currency = resolveCurrency(stockCd, item.currency());

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
