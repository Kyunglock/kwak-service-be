package com.investment.stockadvisor.application.service.guru;

import com.investment.stockadvisor.application.dto.guru.GuruPortfolioResponse;
import com.investment.stockadvisor.application.dto.guru.GuruPortfolioSearchRequest;
import com.investment.stockadvisor.application.dto.guru.GuruRecentActivityResponse;

import java.util.List;

public interface GuruPortfolioService {

    List<GuruPortfolioResponse> findGuruPortfolioAll(GuruPortfolioSearchRequest searchRequest);

    GuruPortfolioResponse findGuruPortfolioById(Long id);

    List<GuruRecentActivityResponse> findLatestActivityPerTicker(String guruCd);
}
