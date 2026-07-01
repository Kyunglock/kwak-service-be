package com.investment.portal.domain.repository.log;

import com.investment.portal.domain.entity.log.ActivityLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityLogMapper {

    void insert(ActivityLog log);

    List<ActivityLog> findByUser(@Param("userId") String userId,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    long countByUser(@Param("userId") String userId);

    List<ActivityLog> search(@Param("userId") String userId,
                             @Param("actionType") String actionType,
                             @Param("offset") int offset,
                             @Param("size") int size);

    long countSearch(@Param("userId") String userId,
                     @Param("actionType") String actionType);
}
