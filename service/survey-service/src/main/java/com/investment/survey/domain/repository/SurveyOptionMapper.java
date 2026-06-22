package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.SurveyOption;
import com.investment.survey.domain.entity.SurveyOptionStats;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyOptionMapper {

    SurveyOption findOptionById(@Param("optionId") Long optionId);

    List<SurveyOption> findOptionsByQuestionId(@Param("questionId") Long questionId);

    List<SurveyOption> findOptionsByQuestionIds(@Param("questionIds") List<Long> questionIds);

    int insertOption(SurveyOption option);

    int updateOption(SurveyOption option);

    int deleteOption(@Param("optionId") Long optionId);

    int deleteOptionsByQuestionId(@Param("questionId") Long questionId);

    List<SurveyOptionStats> findOptionsStatsByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
