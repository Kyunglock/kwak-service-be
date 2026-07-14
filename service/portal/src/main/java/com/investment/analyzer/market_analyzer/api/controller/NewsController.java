package com.investment.analyzer.market_analyzer.api.controller;

import com.investment.analyzer.market_analyzer.application.service.news.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시황 브리핑", description = "collector가 수집한 미국 증시 뉴스 + LLM 시황 요약")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "시황 브리핑 조회",
            description = "가장 최근 AI 시황 요약과 관련 뉴스 기사 목록(최대 5건). 요약이 없으면 data:null.")
    @GetMapping("/market-briefing")
    public ResponseEntity<?> getMarketBriefing() {
        return ResponseUtil.success(newsService.getMarketBriefing(), "시황 브리핑 조회 성공");
    }
}
