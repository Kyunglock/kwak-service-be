package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.CodeGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeGroupMapper {

    CodeGroup findCodeGroupById(@Param("codeGroupId") Long codeGroupId);

    CodeGroup findCodeGroupByCode(@Param("groupCode") String groupCode);

    List<CodeGroup> findAllCodeGroups();

    int insertCodeGroup(CodeGroup codeGroup);

    int updateCodeGroup(CodeGroup codeGroup);

    int deleteCodeGroup(@Param("codeGroupId") Long codeGroupId);
}
