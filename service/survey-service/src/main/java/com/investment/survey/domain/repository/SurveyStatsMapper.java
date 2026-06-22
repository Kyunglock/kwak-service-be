package com.investment.survey.domain.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.investment.survey.domain.entity.SurveyStats;

@Mapper
public interface SurveyStatsMapper {
    List<SurveyStats> findAllSurveyStats(String userId);
}
