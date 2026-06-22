package com.investment.survey.domain.repository;

import com.investment.survey.domain.entity.CodeDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeDetailMapper {

    CodeDetail findCodeDetailById(@Param("codeDetailId") Long codeDetailId);

    List<CodeDetail> findCodeDetailsByGroupId(@Param("codeGroupId") Long codeGroupId);

    List<CodeDetail> findCodeDetailsByGroupCode(@Param("groupCode") String groupCode);

    int insertCodeDetail(CodeDetail codeDetail);

    int updateCodeDetail(CodeDetail codeDetail);

    int deleteCodeDetail(@Param("codeDetailId") Long codeDetailId);
}
