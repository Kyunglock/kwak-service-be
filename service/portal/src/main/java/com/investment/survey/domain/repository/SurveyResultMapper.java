package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.SurveyResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyResultMapper {

    SurveyResult findSurveyResultById(@Param("resultId") Long resultId);

    SurveyResult findActiveSurveyResultByUserAndSurvey(
            @Param("userId") String userId, @Param("surveyId") Long surveyId);

    List<SurveyResult> findSurveyResultsByUserId(@Param("userId") String userId);

    int insertSurveyResult(SurveyResult result);

    int deactivateSurveyResultsByUserAndSurvey(
            @Param("userId") String userId, @Param("surveyId") Long surveyId);
}
