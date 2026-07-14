package com.investment.analyzer.market_analyzer.domain.repository.news;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.investment.analyzer.market_analyzer.domain.entity.news.MarketSummary;
import com.investment.analyzer.market_analyzer.domain.entity.news.NewsArticle;

@Mapper
public interface NewsMapper {

    /** 가장 최근 시황 요약 1건 (주말/휴장일에는 지난 거래일 요약) */
    Optional<MarketSummary> findLatestSummary();

    /** summary_dt 전날 00:00 ~ 당일 24:00(미포함) 발행 기사, 최신순 최대 5건 */
    List<NewsArticle> findArticlesForBriefing(LocalDate summaryDt);
}
