package com.investment.analyzer.market_analyzer.domain.entity.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {
    private Long id;
    private String title;
    private String source;
    private String url;
    private LocalDateTime publishedAt;
}
