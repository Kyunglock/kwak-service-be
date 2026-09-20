package com.investment.portal.application.service.portfolio;

import com.investment.portal.application.dto.portfolio.PortfolioResponse;
import com.investment.portal.application.service.history.TransactionHistoryService;
import com.investment.portal.domain.entity.portfolio.Portfolio;
import com.investment.portal.domain.repository.portfolio.PortfolioItemMapper;
import com.investment.portal.domain.repository.portfolio.PortfolioMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 포트폴리오가 하나도 없는 사용자에게 기본 포트폴리오를 만들어 주는 동작. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortfolioDefaultCreationTest {

    private static final String USER = "user-1";

    @Mock PortfolioMapper portfolioMapper;
    @Mock PortfolioItemMapper portfolioItemMapper;
    @Mock TransactionHistoryService transactionHistoryService;
    @Mock DefaultPortfolioGuard defaultPortfolioGuard;
    @InjectMocks PortfolioServiceImpl service;

    private Portfolio portfolio(long id, String name) {
        return Portfolio.builder()
                .portfolioId(id).userId(USER).portfolioNm(name)
                .baseCurrency("USD").useYn("Y").build();
    }

    @Test
    void 이미_포트폴리오가_있으면_만들지_않는다() {
        when(portfolioMapper.findByUserId(USER))
                .thenReturn(List.of(portfolio(1L, "미국 장기투자")));

        List<PortfolioResponse> result = service.getMyPortfolios(USER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).portfolioNm()).isEqualTo("미국 장기투자");
        verify(portfolioMapper, never()).insert(any());
        verifyNoInteractions(defaultPortfolioGuard);
    }

    @Test
    void 하나도_없으면_기본_포트폴리오를_만들어_반환한다() {
        when(defaultPortfolioGuard.tryAcquire(USER)).thenReturn(true);
        when(portfolioMapper.findByUserId(USER)).thenReturn(
                List.of(),                                                    // 최초 조회
                List.of(),                                                    // 락 획득 후 재확인
                List.of(portfolio(9L, PortfolioServiceImpl.DEFAULT_PORTFOLIO_NM))); // 생성 후

        List<PortfolioResponse> result = service.getMyPortfolios(USER);

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioMapper).insert(captor.capture());
        assertThat(captor.getValue().getPortfolioNm())
                .isEqualTo(PortfolioServiceImpl.DEFAULT_PORTFOLIO_NM);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER);
        assertThat(captor.getValue().getBaseCurrency()).isEqualTo("USD");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).portfolioNm())
                .isEqualTo(PortfolioServiceImpl.DEFAULT_PORTFOLIO_NM);
    }

    @Test
    void 다른_요청이_만드는_중이면_중복_생성하지_않는다() {
        when(defaultPortfolioGuard.tryAcquire(USER)).thenReturn(false);
        when(portfolioMapper.findByUserId(USER)).thenReturn(List.of());

        List<PortfolioResponse> result = service.getMyPortfolios(USER);

        assertThat(result).isEmpty(); // 다음 조회에서 잡힌다
        verify(portfolioMapper, never()).insert(any());
        verify(defaultPortfolioGuard, never()).release(USER);
    }

    @Test
    void 락_획득_직전에_생성됐으면_그것을_반환한다() {
        when(defaultPortfolioGuard.tryAcquire(USER)).thenReturn(true);
        when(portfolioMapper.findByUserId(USER)).thenReturn(
                List.of(),                                   // 최초 조회
                List.of(portfolio(9L, "내 포트폴리오")));      // 락 획득 후 재확인 — 이미 있다

        List<PortfolioResponse> result = service.getMyPortfolios(USER);

        assertThat(result).hasSize(1);
        verify(portfolioMapper, never()).insert(any());
        verify(defaultPortfolioGuard).release(USER);
    }

    @Test
    void 생성_중_예외가_나도_락은_해제된다() {
        when(defaultPortfolioGuard.tryAcquire(USER)).thenReturn(true);
        when(portfolioMapper.findByUserId(USER)).thenReturn(List.of());
        doThrow(new RuntimeException("DB down")).when(portfolioMapper).insert(any());

        try {
            service.getMyPortfolios(USER);
        } catch (RuntimeException ignored) {
            // 예외 전파 자체는 이 테스트의 관심사가 아니다
        }

        verify(defaultPortfolioGuard).release(USER);
    }

    @Test
    void 대시보드도_기본_포트폴리오를_활성으로_잡는다() {
        when(defaultPortfolioGuard.tryAcquire(USER)).thenReturn(true);
        when(portfolioMapper.findByUserId(USER)).thenReturn(
                List.of(),
                List.of(),
                List.of(portfolio(9L, PortfolioServiceImpl.DEFAULT_PORTFOLIO_NM)));
        when(portfolioItemMapper.findWithCompanyByPortfolioId(9L)).thenReturn(List.of());
        when(transactionHistoryService.getTransactionsByPortfolioId(9L)).thenReturn(List.of());

        var dashboard = service.getDashboard(USER);

        assertThat(dashboard.activePortfolioId()).isEqualTo(9L);
        assertThat(dashboard.portfolios()).hasSize(1);
    }
}
