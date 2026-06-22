package com.investment.portal.application.service.history;

import com.investment.portal.application.dto.history.transaction.*;
import com.investment.portal.domain.entity.history.transaction.TransactionHistory;
import com.investment.portal.domain.entity.portfolio.PortfolioItem;
import com.investment.portal.domain.repository.history.TransactionHistoryMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TransactionHistoryMapper transactionHistoryMapper;
    private final PortfolioItemMapper portfolioItemMapper;

    @Override
    public TransactionHistoryResponse getTransaction(Long transId) {
        TransactionHistory history = transactionHistoryMapper.findByTransId(transId);
        if (history == null) {
            return null;
        }
        return toResponse(history);
    }

    @Override
    public List<TransactionHistoryResponse> getTransactionsByPortfolioId(Long portfolioId) {
        return transactionHistoryMapper.findByPortfolioId(portfolioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TransactionHistoryResponse> getTransactionsByPortfolioIdAndStockCd(Long portfolioId, String stockCd) {
        return transactionHistoryMapper.findByPortfolioIdAndStockCd(portfolioId, stockCd).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TransactionHistoryResponse addTransaction(TransactionHistoryAddRequest request) {
        TransactionHistory history = TransactionHistory.builder()
                .portfolioId(request.portfolioId())
                .stockCd(request.stockCd())
                .transType(request.transType())
                .transDt(request.transDt())
                .qty(request.qty())
                .price(request.price())
                .fee(request.fee())
                .tax(request.tax())
                .currency(request.currency() != null ? request.currency() : "USD")
                .memo(request.memo())
                .build();

        transactionHistoryMapper.insert(history);
        log.info("[Transaction] 거래 등록 완료 - transId: {}, type: {}, stockCd: {}",
                history.getTransId(), history.getTransType(), history.getStockCd());

        syncPortfolioItem(request);

        return getTransaction(history.getTransId());
    }

    private void syncPortfolioItem(TransactionHistoryAddRequest request) {
        PortfolioItem existing = portfolioItemMapper.findByPortfolioIdAndStockCd(
                request.portfolioId(), request.stockCd());

        if ("BUY".equals(request.transType())) {
            if (existing == null) {
                // 신규 포트폴리오 종목 등록
                PortfolioItem newItem = PortfolioItem.builder()
                        .portfolioId(request.portfolioId())
                        .stockCd(request.stockCd())
                        .holdQty(request.qty())
                        .buyPrice(request.price())
                        .buyDt(request.transDt())
                        .currency(request.currency() != null ? request.currency() : "USD")
                        .build();
                portfolioItemMapper.insert(newItem);
                log.info("[PortfolioItem] 종목 신규 등록 - portfolioId: {}, stockCd: {}, qty: {}",
                        request.portfolioId(), request.stockCd(), request.qty());
            } else {
                // 기존 종목 수량 증가 + 평균 매수단가 재계산
                BigDecimal existingQty = existing.getHoldQty();
                BigDecimal existingPrice = existing.getBuyPrice();
                BigDecimal newQty = request.qty();
                BigDecimal newPrice = request.price();

                BigDecimal totalQty = existingQty.add(newQty);
                // 평균단가 = (기존수량 * 기존단가 + 신규수량 * 신규단가) / 총수량
                BigDecimal avgPrice = existingQty.multiply(existingPrice)
                        .add(newQty.multiply(newPrice))
                        .divide(totalQty, 4, RoundingMode.HALF_UP);

                PortfolioItem updated = PortfolioItem.builder()
                        .itemId(existing.getItemId())
                        .holdQty(totalQty)
                        .buyPrice(avgPrice)
                        .build();
                portfolioItemMapper.update(updated);
                log.info("[PortfolioItem] 종목 수량 증가 - itemId: {}, stockCd: {}, {}+{}={}, avgPrice: {}",
                        existing.getItemId(), request.stockCd(), existingQty, newQty, totalQty, avgPrice);
            }
        } else if ("SELL".equals(request.transType())) {
            if (existing == null) {
                throw new IllegalArgumentException(
                        "매도할 종목이 포트폴리오에 없습니다: portfolioId=" + request.portfolioId()
                                + ", stockCd=" + request.stockCd());
            }

            BigDecimal remainQty = existing.getHoldQty().subtract(request.qty());

            if (remainQty.compareTo(BigDecimal.ZERO) <= 0) {
                // 전량 매도 → 포트폴리오 종목 삭제 (논리 삭제)
                portfolioItemMapper.delete(existing.getItemId());
                log.info("[PortfolioItem] 전량 매도 → 종목 삭제 - itemId: {}, stockCd: {}",
                        existing.getItemId(), request.stockCd());
            } else {
                // 일부 매도 → 수량 감소 (매수단가 유지)
                PortfolioItem updated = PortfolioItem.builder()
                        .itemId(existing.getItemId())
                        .holdQty(remainQty)
                        .build();
                portfolioItemMapper.update(updated);
                log.info("[PortfolioItem] 일부 매도 → 수량 감소 - itemId: {}, stockCd: {}, 잔여: {}",
                        existing.getItemId(), request.stockCd(), remainQty);
            }
        }
    }

    /**
     * 해당 종목의 모든 거래를 시간순으로 재생하여 보유종목 상태를 재계산
     */
    private void recalculatePortfolioItem(Long portfolioId, String stockCd) {
        List<TransactionHistory> transactions = transactionHistoryMapper.findByPortfolioIdAndStockCd(portfolioId, stockCd);
        transactions.sort(Comparator.comparing(TransactionHistory::getTransDt)
                .thenComparing(TransactionHistory::getTransId));

        PortfolioItem existing = portfolioItemMapper.findByPortfolioIdAndStockCd(portfolioId, stockCd);

        BigDecimal holdQty = BigDecimal.ZERO;
        BigDecimal avgPrice = BigDecimal.ZERO;
        LocalDate buyDt = null;
        String currency = "USD";

        for (TransactionHistory tx : transactions) {
            if ("BUY".equals(tx.getTransType())) {
                BigDecimal newTotalQty = holdQty.add(tx.getQty());
                if (newTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    avgPrice = holdQty.multiply(avgPrice)
                            .add(tx.getQty().multiply(tx.getPrice()))
                            .divide(newTotalQty, 4, RoundingMode.HALF_UP);
                }
                holdQty = newTotalQty;
                buyDt = tx.getTransDt();
                currency = tx.getCurrency() != null ? tx.getCurrency() : "USD";
            } else if ("SELL".equals(tx.getTransType())) {
                holdQty = holdQty.subtract(tx.getQty());
                if (holdQty.compareTo(BigDecimal.ZERO) <= 0) {
                    holdQty = BigDecimal.ZERO;
                    avgPrice = BigDecimal.ZERO;
                }
            }
        }

        if (holdQty.compareTo(BigDecimal.ZERO) <= 0) {
            if (existing != null) {
                portfolioItemMapper.delete(existing.getItemId());
                log.info("[PortfolioItem] 재계산 → 종목 삭제 - portfolioId: {}, stockCd: {}", portfolioId, stockCd);
            }
        } else {
            if (existing == null) {
                PortfolioItem newItem = PortfolioItem.builder()
                        .portfolioId(portfolioId)
                        .stockCd(stockCd)
                        .holdQty(holdQty)
                        .buyPrice(avgPrice)
                        .buyDt(buyDt)
                        .currency(currency)
                        .build();
                portfolioItemMapper.insert(newItem);
                log.info("[PortfolioItem] 재계산 → 종목 신규 등록 - portfolioId: {}, stockCd: {}, qty: {}, avgPrice: {}",
                        portfolioId, stockCd, holdQty, avgPrice);
            } else {
                PortfolioItem updated = PortfolioItem.builder()
                        .itemId(existing.getItemId())
                        .holdQty(holdQty)
                        .buyPrice(avgPrice)
                        .build();
                portfolioItemMapper.update(updated);
                log.info("[PortfolioItem] 재계산 → 종목 수정 - itemId: {}, stockCd: {}, qty: {}, avgPrice: {}",
                        existing.getItemId(), stockCd, holdQty, avgPrice);
            }
        }
    }

    @Override
    @Transactional
    public TransactionHistoryResponse modifyTransaction(TransactionHistoryModRequest request) {
        TransactionHistory existing = transactionHistoryMapper.findByTransId(request.transId());
        if (existing == null) {
            throw new IllegalArgumentException("해당 거래를 찾을 수 없습니다: " + request.transId());
        }

        TransactionHistory history = TransactionHistory.builder()
                .transId(request.transId())
                .transDt(request.transDt())
                .qty(request.qty())
                .price(request.price())
                .memo(request.memo())
                .build();

        transactionHistoryMapper.update(history);
        log.info("[Transaction] 거래 수정 완료 - transId: {}", request.transId());

        recalculatePortfolioItem(existing.getPortfolioId(), existing.getStockCd());

        return getTransaction(request.transId());
    }

    @Override
    @Transactional
    public void removeTransaction(Long transId) {
        TransactionHistory existing = transactionHistoryMapper.findByTransId(transId);
        if (existing == null) {
            throw new IllegalArgumentException("해당 거래를 찾을 수 없습니다: " + transId);
        }

        transactionHistoryMapper.delete(transId);
        log.info("[Transaction] 거래 삭제 완료 - transId: {}", transId);

        recalculatePortfolioItem(existing.getPortfolioId(), existing.getStockCd());
    }

    private TransactionHistoryResponse toResponse(TransactionHistory h) {
        return new TransactionHistoryResponse(
                h.getTransId(),
                h.getPortfolioId(),
                h.getStockCd(),
                h.getTransType(),
                h.getTransDt(),
                h.getQty(),
                h.getPrice(),
                h.getAmount(),
                h.getFee(),
                h.getTax(),
                h.getCurrency(),
                h.getMemo(),
                h.getRegDt()
        );
    }
}
