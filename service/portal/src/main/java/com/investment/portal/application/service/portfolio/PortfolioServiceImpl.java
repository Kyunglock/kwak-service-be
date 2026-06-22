package com.investment.portal.application.service.portfolio;

import com.investment.portal.application.dto.portfolio.*;
import com.investment.portal.domain.entity.portfolio.Portfolio;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioMapper portfolioMapper;

    @Override
    public PortfolioResponse getPortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioMapper.findByPortfolioId(portfolioId);
        if (portfolio == null) {
            return null;
        }
        return toResponse(portfolio);
    }

    @Override
    public List<PortfolioResponse> getPortfoliosByUserId(String userId) {
        return portfolioMapper.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PortfolioResponse> getMyPortfolios(String userId) {
        return getPortfoliosByUserId(userId);
    }

    @Override
    public PortfolioResponse addPortfolio(String userId, PortfolioAddRequest request) {
        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .portfolioNm(request.portfolioNm())
                .portfolioDesc(request.portfolioDesc())
                .baseCurrency(request.baseCurrency() != null ? request.baseCurrency() : "USD")
                .build();

        portfolioMapper.insert(portfolio);
        log.info("[Portfolio] 포트폴리오 등록 완료 - portfolioId: {}, userId: {}", portfolio.getPortfolioId(), userId);

        return getPortfolio(portfolio.getPortfolioId());
    }

    @Override
    public PortfolioResponse modifyPortfolio(PortfolioModRequest request) {
        Portfolio existing = portfolioMapper.findByPortfolioId(request.portfolioId());
        if (existing == null) {
            throw new IllegalArgumentException("해당 포트폴리오를 찾을 수 없습니다: " + request.portfolioId());
        }

        Portfolio portfolio = Portfolio.builder()
                .portfolioId(request.portfolioId())
                .portfolioNm(request.portfolioNm())
                .portfolioDesc(request.portfolioDesc())
                .baseCurrency(request.baseCurrency())
                .build();

        portfolioMapper.update(portfolio);
        log.info("[Portfolio] 포트폴리오 수정 완료 - portfolioId: {}", request.portfolioId());

        return getPortfolio(request.portfolioId());
    }

    @Override
    public void removePortfolio(Long portfolioId) {
        Portfolio existing = portfolioMapper.findByPortfolioId(portfolioId);
        if (existing == null) {
            throw new IllegalArgumentException("해당 포트폴리오를 찾을 수 없습니다: " + portfolioId);
        }

        portfolioMapper.delete(portfolioId);
        log.info("[Portfolio] 포트폴리오 삭제 완료 - portfolioId: {}", portfolioId);
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.getPortfolioId(),
                portfolio.getUserId(),
                portfolio.getPortfolioNm(),
                portfolio.getPortfolioDesc(),
                portfolio.getBaseCurrency(),
                portfolio.getUseYn(),
                portfolio.getRegDt(),
                portfolio.getUpdDt()
        );
    }
}
