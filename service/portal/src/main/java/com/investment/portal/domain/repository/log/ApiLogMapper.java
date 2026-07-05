package com.investment.portal.domain.repository.log;

import com.investment.portal.domain.entity.log.ApiLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiLogMapper {

    void insert(ApiLog log);
}
