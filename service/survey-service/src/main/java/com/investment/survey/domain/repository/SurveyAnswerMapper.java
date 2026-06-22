package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.SurveyAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyAnswerMapper {

    List<SurveyAnswer> findAnswersByResponseId(@Param("responseId") Long responseId);

    int insertAnswer(SurveyAnswer answer);

    int batchInsertAnswers(@Param("list") List<SurveyAnswer> answers);

    int deleteAnswersByResponseId(@Param("responseId") Long responseId);
}
