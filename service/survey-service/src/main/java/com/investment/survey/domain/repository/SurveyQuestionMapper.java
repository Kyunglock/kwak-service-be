package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.QuestionWithAnswer;
import com.investment.survey.domain.entity.SurveyQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyQuestionMapper {

    SurveyQuestion findQuestionById(@Param("questionId") Long questionId);

    List<SurveyQuestion> findQuestionsBySurveyId(@Param("surveyId") Long surveyId);

    List<QuestionWithAnswer> findQuestionsWithAnswers(
        @Param("surveyId") Long surveyId,
        @Param("responseId") Long responseId,
        @Param("userId") String userId
    );

    int insertQuestion(SurveyQuestion question);

    int updateQuestion(SurveyQuestion question);

    int deleteQuestion(@Param("questionId") Long questionId);

    int deleteQuestionsBySurveyId(@Param("surveyId") Long surveyId);
}
