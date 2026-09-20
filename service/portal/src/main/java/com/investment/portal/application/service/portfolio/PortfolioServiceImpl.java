package com.investment.portal.application.service.portfolio;

import com.investment.portal.application.dto.history.transaction.TransactionHistoryResponse;
import com.investment.portal.application.dto.portfolio.*;
import com.investment.portal.application.dto.portfolio.item.PortfolioPositionDto;
import com.investment.portal.application.service.history.TransactionHistoryService;
import com.investment.portal.domain.entity.portfolio.Portfolio;
import com.investment.portal.domain.repository.portfolio.PortfolioItemMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    /** 가입 직후 자동으로 만들어 주는 포트폴리오 이름 */
    static final String DEFAULT_PORTFOLIO_NM = "내 포트폴리오";

    private final PortfolioMapper portfolioMapper;
    private final PortfolioItemMapper portfolioItemMapper;
    private final TransactionHistoryService transactionHistoryService;
    private final DefaultPortfolioGuard defaultPortfolioGuard;

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

    /**
     * 내 포트폴리오 목록. 하나도 없으면 기본 포트폴리오를 만들어 함께 반환한다.
     *
     * <p>포트폴리오가 없으면 매매 기록을 어디에도 담을 수 없어, 사용자가 무엇을 하든
     * "먼저 포트폴리오를 만드세요"에서 막힌다. 첫 화면에서 요구할 만한 설정이 아니다.
     */
    @Override
    public List<PortfolioResponse> getMyPortfolios(String userId) {
        List<PortfolioResponse> portfolios = getPortfoliosByUserId(userId);
        return portfolios.isEmpty() ? createDefaultPortfolio(userId) : portfolios;
    }

    private List<PortfolioResponse> createDefaultPortfolio(String userId) {
        if (!defaultPortfolioGuard.tryAcquire(userId)) {
            // 다른 요청이 만드는 중 — 중복 생성 대신 현재 상태를 그대로 돌려준다.
            // 아직 안 보이더라도 다음 조회에서 잡힌다.
            return getPortfoliosByUserId(userId);
        }
        try {
            // 락을 기다리는 사이에 만들어졌을 수 있다
            List<PortfolioResponse> existing = getPortfoliosByUserId(userId);
            if (!existing.isEmpty()) {
                return existing;
            }
            addPortfolio(userId, new PortfolioAddRequest(DEFAULT_PORTFOLIO_NM, null, "USD"));
            log.info("[Portfolio] 기본 포트폴리오 자동 생성 - userId: {}", userId);
            return getPortfoliosByUserId(userId);
        } finally {
            defaultPortfolioGuard.release(userId);
        }
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

    @Override
    public PortfolioDashboardResponse getDashboard(String userId) {
        // getMyPortfolios 가 비어 있으면 기본 포트폴리오를 만들어 준다.
        // 그래도 비어 있는 경우는 동시 생성 경합뿐이며, 다음 조회에서 정상화된다.
        List<PortfolioResponse> portfolios = getMyPortfolios(userId);
        if (portfolios.isEmpty()) {
            return new PortfolioDashboardResponse(portfolios, null, List.of(), List.of());
        }
        Long activeId = portfolios.get(0).portfolioId();
        List<PortfolioPositionDto> positions = portfolioItemMapper.findWithCompanyByPortfolioId(activeId);
        List<TransactionHistoryResponse> transactions = transactionHistoryService.getTransactionsByPortfolioId(activeId);
        return new PortfolioDashboardResponse(portfolios, activeId, positions, transactions);
    }

    @Override
    public PortfolioDetailResponse getDetail(Long portfolioId) {
        List<PortfolioPositionDto> positions = portfolioItemMapper.findWithCompanyByPortfolioId(portfolioId);
        List<TransactionHistoryResponse> transactions = transactionHistoryService.getTransactionsByPortfolioId(portfolioId);
        return new PortfolioDetailResponse(portfolioId, positions, transactions);
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
