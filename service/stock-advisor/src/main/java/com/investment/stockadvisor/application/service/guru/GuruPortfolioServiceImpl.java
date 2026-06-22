package com.investment.stockadvisor.application.service.guru;

import com.investment.stockadvisor.application.dto.guru.GuruPortfolioResponse;
import com.investment.stockadvisor.application.dto.guru.GuruPortfolioSearchRequest;
import com.investment.stockadvisor.application.dto.guru.GuruRecentActivityResponse;
import com.investment.stockadvisor.domain.entity.guru.GuruPortfolio;
import com.investment.stockadvisor.domain.entity.guru.GuruRecentActivity;
import com.investment.stockadvisor.domain.repository.guru.GuruPortfoliosMapper;
import com.investment.stockadvisor.domain.repository.guru.GuruRecentActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuruPortfolioServiceImpl implements GuruPortfolioService {

    private final GuruPortfoliosMapper guruPortfoliosMapper;
    private final GuruRecentActivityMapper guruRecentActivityMapper;
    private final GuruConverter guruConverter;

    @Override
    public List<GuruPortfolioResponse> findGuruPortfolioAll(GuruPortfolioSearchRequest searchRequest) {
        List<GuruPortfolio> list = guruPortfoliosMapper.findGuruPortfolioAll(searchRequest);
        return guruConverter.toPortfolioResponseList(list);
    }

    @Override
    public GuruPortfolioResponse findGuruPortfolioById(Long id) {
        GuruPortfolio entity = guruPortfoliosMapper.findById(id);
        return guruConverter.toPortfolioResponse(entity);
    }

    @Override
    public List<GuruRecentActivityResponse> findLatestActivityPerTicker(String guruCd) {
        List<GuruRecentActivity> list = guruRecentActivityMapper.findLatestActivityPerTicker(guruCd);
        return guruConverter.toActivityResponseList(list);
    }
}
