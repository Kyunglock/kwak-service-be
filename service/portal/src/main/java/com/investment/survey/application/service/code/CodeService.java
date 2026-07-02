package com.investment.survey.application.service.code;

import com.investment.survey.application.dto.code.*;

import java.util.List;

public interface CodeService {

    List<CodeGroupResponse> getCodeGroups();

    CodeGroupResponse getCodeGroup(String groupCode);

    CodeGroupResponse addCodeGroup(CodeGroupAddRequest request);

    void removeCodeGroup(Long codeGroupId);

    List<CodeDetailResponse> getCodeDetails(String groupCode);

    CodeDetailResponse addCodeDetail(CodeDetailAddRequest request);

    void removeCodeDetail(Long codeDetailId);
}
