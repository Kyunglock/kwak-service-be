package com.investment.portal.domain.repository.fortune;

import com.investment.portal.domain.entity.fortune.StockFortune;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Optional;

@Mapper
public interface FortuneMapper {

    /** (ticker, 기준일) 운세 단건 조회 */
    Optional<StockFortune> findByTickerAndDate(@Param("ticker") String ticker,
                                               @Param("fortuneDate") LocalDate fortuneDate);

    /** 운세 저장. uk_fortune_ticker_date 경합 시 DuplicateKeyException */
    void insert(StockFortune fortune);

    /**
     * 입력 티커의 정식형 조회 (미등록이면 empty).
     * US: tbl_companies.ticker 그대로. KR: STOCK_CD 또는 suffix형(005930.KS/KQ) 입력 모두
     * 정식형(005930.KS)으로 반환 — 캐시 키를 시스템 표준(Yahoo suffix) 형식으로 통일.
     */
    Optional<String> findCanonicalTicker(@Param("ticker") String ticker);

    /**
     * 종목명 정확 일치(UPPER 비교)로 정식 티커 조회 (미등록이면 empty).
     * KR 한글 종목명("삼성전자") 및 영문 종목명("NAVER", "SK하이닉스") 대상 — tbl_stock_info.STOCK_NM.
     */
    Optional<String> findCanonicalTickerByName(@Param("name") String name);

    /**
     * 정식 티커의 종목명 조회 (US: tbl_companies.company_name, KR: tbl_stock_info.STOCK_NM).
     * LLM 프롬프트 표기용 — LLM이 티커만 보고 회사명을 잘못 지어내는 것 방지.
     */
    Optional<String> findStockNameByTicker(@Param("ticker") String ticker);
}
