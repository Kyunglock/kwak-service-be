package com.investment.portal.domain.repository.history;

import com.investment.portal.domain.entity.history.transaction.TransactionHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionHistoryMapper {

    /**
     * 거래ID로 조회
     */
    TransactionHistory findByTransId(@Param("transId") Long transId);

    /**
     * 포트폴리오ID로 거래 이력 조회
     */
    List<TransactionHistory> findByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오ID + 종목코드로 거래 이력 조회
     */
    List<TransactionHistory> findByPortfolioIdAndStockCd(
            @Param("portfolioId") Long portfolioId,
            @Param("stockCd") String stockCd);

    /**
     * 거래 이력 등록
     */
    int insert(TransactionHistory transactionHistory);

    /**
     * 거래 이력 수정
     */
    int update(TransactionHistory transactionHistory);

    /**
     * 거래 이력 삭제
     */
    int delete(@Param("transId") Long transId);
}
