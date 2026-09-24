package com.investment.analyzer.market_analyzer.domain.repository.news;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * 대상일 전후에서 keywords 중 하나라도 title/content 에 포함된 기사를 최대 5건,
     * 장 마감 시각(closeAt, KST)에 가까운 순으로.
     *
     * <p>published_at 은 KST 로 저장된다. 미국 종목의 대상일은 미국 거래일이라
     * 장 마감 직후 기사가 KST "다음날 새벽"에 찍히므로 대상일+1일까지 포함한다.
     */
    List<MarketNewsRow> findByDateAndKeyword(@Param("date") LocalDate date,
                                             @Param("closeAt") LocalDateTime closeAt,
                                             @Param("keywords") List<String> keywords);
}
