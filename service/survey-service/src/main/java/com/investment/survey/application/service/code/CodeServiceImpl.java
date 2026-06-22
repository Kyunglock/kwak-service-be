package com.investment.survey.application.service.code;

import com.investment.survey.application.dto.code.*;
import com.investment.survey.domain.entity.CodeDetail;
import com.investment.survey.domain.entity.CodeGroup;
import com.investment.survey.domain.repository.CodeDetailMapper;
import com.investment.survey.domain.repository.CodeGroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeServiceImpl implements CodeService {

    private final CodeGroupMapper codeGroupMapper;
    private final CodeDetailMapper codeDetailMapper;

    @Override
    public List<CodeGroupResponse> getCodeGroups() {
        return codeGroupMapper.findAllCodeGroups().stream()
                .map(g -> toGroupResponse(g, codeDetailMapper.findCodeDetailsByGroupId(g.getCodeGroupId())))
                .toList();
    }

    @Override
    public CodeGroupResponse getCodeGroup(String groupCode) {
        CodeGroup group = codeGroupMapper.findCodeGroupByCode(groupCode);
        if (group == null) {
            throw new IllegalArgumentException("코드 그룹을 찾을 수 없습니다: " + groupCode);
        }
        List<CodeDetail> details = codeDetailMapper.findCodeDetailsByGroupId(group.getCodeGroupId());
        return toGroupResponse(group, details);
    }

    @Override
    public CodeGroupResponse addCodeGroup(CodeGroupAddRequest request) {
        CodeGroup group = CodeGroup.builder()
                .groupCode(request.groupCode())
                .groupName(request.groupName())
                .description(request.description())
                .sortOrder(request.sortOrder())
                .build();
        codeGroupMapper.insertCodeGroup(group);
        log.info("[Code] 코드 그룹 등록 - groupCode: {}", request.groupCode());
        return toGroupResponse(group, Collections.emptyList());
    }

    @Override
    public void removeCodeGroup(Long codeGroupId) {
        codeGroupMapper.deleteCodeGroup(codeGroupId);
        log.info("[Code] 코드 그룹 삭제 - codeGroupId: {}", codeGroupId);
    }

    @Override
    public List<CodeDetailResponse> getCodeDetails(String groupCode) {
        return codeDetailMapper.findCodeDetailsByGroupCode(groupCode).stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Override
    public CodeDetailResponse addCodeDetail(CodeDetailAddRequest request) {
        CodeDetail detail = CodeDetail.builder()
                .codeGroupId(request.codeGroupId())
                .codeValue(request.codeValue())
                .codeName(request.codeName())
                .codeDesc(request.codeDesc())
                .sortOrder(request.sortOrder())
                .build();
        codeDetailMapper.insertCodeDetail(detail);
        log.info("[Code] 코드 상세 등록 - codeValue: {}", request.codeValue());
        return toDetailResponse(detail);
    }

    @Override
    public void removeCodeDetail(Long codeDetailId) {
        codeDetailMapper.deleteCodeDetail(codeDetailId);
        log.info("[Code] 코드 상세 삭제 - codeDetailId: {}", codeDetailId);
    }

    private CodeGroupResponse toGroupResponse(CodeGroup g, List<CodeDetail> details) {
        return new CodeGroupResponse(
                g.getCodeGroupId(), g.getGroupCode(), g.getGroupName(),
                g.getDescription(), g.getSortOrder(), g.getRegDt(), g.getUpdDt(),
                details.stream().map(this::toDetailResponse).toList()
        );
    }

    private CodeDetailResponse toDetailResponse(CodeDetail d) {
        return new CodeDetailResponse(
                d.getCodeDetailId(), d.getCodeGroupId(),
                d.getCodeValue(), d.getCodeName(), d.getCodeDesc(), d.getSortOrder()
        );
    }
}
