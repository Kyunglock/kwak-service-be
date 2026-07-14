package com.investment.analyzer.market_analyzer.application.dto.news;

import com.investment.analyzer.market_analyzer.domain.entity.news.MarketSummary;
import com.investment.analyzer.market_analyzer.domain.entity.news.NewsArticle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MarketBriefingResponse(
        LocalDate summaryDt, String summary, String sentiment, List<ArticleItem> articles) {

    public record ArticleItem(String title, String source, String url, LocalDateTime publishedAt) {
        public static ArticleItem from(NewsArticle article) {
            return new ArticleItem(article.getTitle(), article.getSource(),
                    article.getUrl(), article.getPublishedAt());
        }
    }

    public static MarketBriefingResponse of(MarketSummary summary, List<NewsArticle> articles) {
        return new MarketBriefingResponse(summary.getSummaryDt(), summary.getSummary(),
                summary.getSentiment(), articles.stream().map(ArticleItem::from).toList());
    }
}
