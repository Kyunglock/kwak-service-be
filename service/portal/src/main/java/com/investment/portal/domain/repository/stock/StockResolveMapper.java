package com.investment.portal.domain.repository.stock;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자·LLM이 말한 종목 표기를 DB의 정식 티커로 해석한다.
 *
 * <p>LLM이 티커를 지어내는 것을 막는 유일한 방어선이다 — 여기서 찾지 못한 표기는
 * 절대 저장 경로로 넘기지 않는다. FortuneMapper 에도 비슷한 조회가 있지만 그쪽은
 * 한국 종목명(tbl_stock_info)만 다뤄서 "애플" 같은 미국 종목 한글명을 찾지 못한다.
 */
@Mapper
public interface StockResolveMapper {

    /** 티커 형식 입력의 정식형 조회 (US: tbl_companies, KR: suffix 정식형). */
    Optional<String> findCanonicalTicker(@Param("ticker") String ticker);

    /** 종목명 정확 일치(UPPER 비교). 동명이의가 있을 수 있어 목록으로 받는다. */
    List<StockRef> findByExactName(@Param("name") String name);

    /** 종목명 부분 일치 — 정확 일치 실패 시 사용자에게 보여줄 후보. */
    List<StockRef> findByPartialName(@Param("keyword") String keyword, @Param("limit") int limit);

    /** 정식 티커 → 표시용 종목명. */
    Optional<String> findStockNameByTicker(@Param("ticker") String ticker);
}
