package com.investment.portal.domain.repository.log;

import com.investment.portal.domain.entity.log.MenuLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MenuLogMapper {

    void insert(MenuLog log);
}
