package com.investment.portal.domain.repository.survey;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SurveyMapper {

    /**
     * 사용자의 RISK_PROFILE 설문 항목별 점수 조회
     * 반환 키: description (String), score (Double)
     */
    List<Map<String, Object>> findRiskProfileScores(@Param("userId") String userId);
}
