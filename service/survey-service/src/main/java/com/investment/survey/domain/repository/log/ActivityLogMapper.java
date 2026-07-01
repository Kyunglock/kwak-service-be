package com.investment.survey.domain.repository.log;

import com.investment.survey.domain.entity.log.ActivityLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityLogMapper {
    void insert(ActivityLog log);
}
