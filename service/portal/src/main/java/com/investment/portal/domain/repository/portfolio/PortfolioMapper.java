package com.investment.portal.domain.repository.portfolio;

import com.investment.portal.domain.entity.portfolio.Portfolio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PortfolioMapper {

    /**
     * 포트폴리오ID로 조회
     */
    Portfolio findByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 사용자ID로 포트폴리오 목록 조회
     */
    List<Portfolio> findByUserId(@Param("userId") String userId);

    /**
     * 포트폴리오 등록
     */
    int insert(Portfolio portfolio);

    /**
     * 포트폴리오 수정
     */
    int update(Portfolio portfolio);

    /**
     * 포트폴리오 삭제 (USE_YN = 'N')
     */
    int delete(@Param("portfolioId") Long portfolioId);
}
