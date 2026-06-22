package com.investment.portal.domain.repository.insight;

import com.investment.portal.domain.entity.insight.InsightResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InsightResultMapper {

    List<InsightResult> findAllByUserId(@Param("userId") String userId);

    InsightResult findByUserIdAndType(@Param("userId") String userId,
                                      @Param("resultTypeCd") String resultTypeCd);

    void upsert(InsightResult insightResult);
}
