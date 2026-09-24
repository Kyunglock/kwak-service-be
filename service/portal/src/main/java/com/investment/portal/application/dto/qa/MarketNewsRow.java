package com.investment.portal.application.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시황 질의응답용 뉴스 한 줄.
 *
 * <p>{@code market_analyzer.NewsArticle}과 달리 content(스니펫/본문)를 포함한다.
 * NewsArticle 은 브리핑(제목만으로 충분)에 쓰이는 기존 엔티티라 그대로 재사용하지
 * 않고, 이 기능 전용으로 따로 둔다 — 기존 소비처(MarketBriefingResponse 등)의
 * 응답 모양에 영향을 주지 않기 위함이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketNewsRow {
    private Long          id;
    private String        title;
    private String        content;   // 본문 또는 스니펫. 없으면 빈 문자열/null
    private String        source;
    private String        url;
    private LocalDateTime publishedAt;
}
