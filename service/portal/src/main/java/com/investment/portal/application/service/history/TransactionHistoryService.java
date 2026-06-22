package com.investment.portal.application.service.history;

import com.investment.portal.application.dto.history.transaction.*;

import java.util.List;

public interface TransactionHistoryService {

    /**
     * 거래 단건 조회
     */
    TransactionHistoryResponse getTransaction(Long transId);

    /**
     * 포트폴리오별 거래 이력 조회
     */
    List<TransactionHistoryResponse> getTransactionsByPortfolioId(Long portfolioId);

    /**
     * 포트폴리오 + 종목별 거래 이력 조회
     */
    List<TransactionHistoryResponse> getTransactionsByPortfolioIdAndStockCd(Long portfolioId, String stockCd);

    /**
     * 거래 등록
     */
    TransactionHistoryResponse addTransaction(TransactionHistoryAddRequest request);

    /**
     * 거래 수정
     */
    TransactionHistoryResponse modifyTransaction(TransactionHistoryModRequest request);

    /**
     * 거래 삭제
     */
    void removeTransaction(Long transId);
}
