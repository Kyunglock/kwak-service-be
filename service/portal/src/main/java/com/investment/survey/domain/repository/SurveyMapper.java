package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.Survey;
import com.investment.survey.domain.entity.SurveyResponse;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyMapper {

    Survey findSurveyById(@Param("surveyId") Long surveyId);

    List<Survey> findAllSurveys();

    List<Survey> findSurveysByTypeCode(@Param("surveyTypeCode") String surveyTypeCode);

    List<SurveyResponse> findSurveyWithMyResponses(@Param("userId") String userId);

    List<SurveyResponse> findSurveyWithMyResponsesPaged(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("orderBy") String orderBy,
            @Param("orderDir") String orderDir);

    int countSurveyWithMyResponses(
            @Param("userId") String userId,
            @Param("keyword") String keyword);

    int insertSurvey(Survey survey);

    int updateSurvey(Survey survey);

    int deleteSurvey(@Param("surveyId") Long surveyId);
}
