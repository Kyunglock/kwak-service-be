package com.investment.analyzer.market_analyzer.application.service.news;

import com.investment.analyzer.market_analyzer.application.dto.news.MarketBriefingResponse;

public interface NewsService {

    /** 최근 시황 브리핑. 요약이 아예 없으면 null (컨트롤러가 data:null로 응답). */
    MarketBriefingResponse getMarketBriefing();
}
