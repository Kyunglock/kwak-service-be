package com.investment.stockadvisor.domain.repository.guru;

import com.investment.stockadvisor.application.dto.guru.GuruRecentActivitySearchRequest;
import com.investment.stockadvisor.domain.entity.guru.GuruRecentActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GuruRecentActivityMapper {

    List<GuruRecentActivity> findGuruRecentActivityAll(@Param("searchRequest") GuruRecentActivitySearchRequest searchRequest);

    List<GuruRecentActivity> findLatestActivityPerTicker(@Param("guruCd") String guruCd);

    GuruRecentActivity findById(@Param("id") Long id);
}
