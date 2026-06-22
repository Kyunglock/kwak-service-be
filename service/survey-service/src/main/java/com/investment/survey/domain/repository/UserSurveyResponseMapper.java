package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.UserSurveyResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSurveyResponseMapper {

    UserSurveyResponse findSurveyResponseById(@Param("responseId") Long responseId);

    List<UserSurveyResponse> findSurveyResponsesByUserId(@Param("userId") String userId);

    List<UserSurveyResponse> findSurveyResponsesByUserAndSurvey(
            @Param("userId") String userId, @Param("surveyId") Long surveyId);

    int insertSurveyResponse(UserSurveyResponse response);

    int updateSurveyResponse(UserSurveyResponse response);
}
