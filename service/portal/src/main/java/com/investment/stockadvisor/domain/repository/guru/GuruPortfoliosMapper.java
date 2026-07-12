package com.investment.stockadvisor.domain.repository.guru;

import com.investment.stockadvisor.application.dto.guru.GuruPortfolioSearchRequest;
import com.investment.stockadvisor.domain.entity.guru.GuruPortfolio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GuruPortfoliosMapper {

    List<GuruPortfolio> findGuruPortfolioAll(@Param("searchRequest") GuruPortfolioSearchRequest searchRequest);

    GuruPortfolio findById(@Param("id") Long id);
}
