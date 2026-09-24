package com.investment.analyzer.market_analyzer.domain.repository.news;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import com.investment.analyzer.market_analyzer.domain.entity.news.MarketSummary;
import com.investment.analyzer.market_analyzer.domain.entity.news.NewsArticle;
import com.investment.portal.application.dto.qa.MarketNewsRow;

@Mapper
public interface NewsMapper {

    /** 가장 최근 시황 요약 1건 (주말/휴장일에는 지난 거래일 요약) */
    Optional<MarketSummary> findLatestSummary();

    /** summary_dt 전날 00:00 ~ 당일 24:00(미포함) 발행 기사, 최신순 최대 5건 */
    List<NewsArticle> findArticlesForBriefing(LocalDate summaryDt);

    /**
     * 대상일 전후 1일 범위에서 keywords(종목명 등) 중 하나라도 title/content 에
     * 포함된 기사를 최신순 최대 5건. RSS 수집이 22:00~06:00 KST 창을 쓰므로
     * 미국 장중 사건이 "다음날 새벽 KST" 기사로 잡힐 수 있어 하루 버퍼를 둔다
     * ({@link #findArticlesForBriefing} 과 동일한 전제).
     */
    List<MarketNewsRow> findByDateAndKeyword(@Param("date") LocalDate date, @Param("keywords") List<String> keywords);
}
