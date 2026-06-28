package com.investment.survey.domain.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.investment.survey.domain.entity.SurveyStats;

@Mapper
public interface SurveyStatsMapper {
    List<SurveyStats> findAllSurveyStats(String userId);

    List<SurveyStats> findAllSurveyStatsPaged(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("orderBy") String orderBy,
            @Param("orderDir") String orderDir);

    int countSurveyStats(
            @Param("userId") String userId,
            @Param("keyword") String keyword);
}
