package com.investment.survey.api.controller;

import com.investment.survey.application.dto.code.*;
import com.investment.survey.application.service.code.CodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.application.dto.RokResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공통코드", description = "공통코드 그룹/상세 관리 API")
@RestController
@RequestMapping("/api/v1/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    @Operation(summary = "코드 그룹 전체 조회")
    @GetMapping("/groups")
    public ResponseEntity<RokResponse<List<CodeGroupResponse>>> getCodeGroups() {
        return ResponseUtil.success(codeService.getCodeGroups(), "조회 성공");
    }

    @Operation(summary = "코드 그룹 상세 조회 (그룹코드 기준)")
    @GetMapping("/groups/{groupCode}")
    public ResponseEntity<RokResponse<CodeGroupResponse>> getCodeGroup(@PathVariable String groupCode) {
        return ResponseUtil.success(codeService.getCodeGroup(groupCode), "조회 성공");
    }

    @Operation(summary = "코드 그룹 등록")
    @PostMapping("/groups")
    public ResponseEntity<RokResponse<CodeGroupResponse>> addCodeGroup(@Valid @RequestBody CodeGroupAddRequest request) {
        return ResponseUtil.created(codeService.addCodeGroup(request), "등록 성공");
    }

    @Operation(summary = "코드 그룹 삭제")
    @DeleteMapping("/groups/{codeGroupId}")
    public ResponseEntity<?> removeCodeGroup(@PathVariable Long codeGroupId) {
        codeService.removeCodeGroup(codeGroupId);
        return ResponseUtil.noContent();
    }

    @Operation(summary = "그룹코드별 코드 상세 조회")
    @GetMapping("/{groupCode}")
    public ResponseEntity<RokResponse<List<CodeDetailResponse>>> getCodeDetails(@PathVariable String groupCode) {
        return ResponseUtil.success(codeService.getCodeDetails(groupCode), "조회 성공");
    }

    @Operation(summary = "코드 상세 등록")
    @PostMapping("/details")
    public ResponseEntity<RokResponse<CodeDetailResponse>> addCodeDetail(@Valid @RequestBody CodeDetailAddRequest request) {
        return ResponseUtil.created(codeService.addCodeDetail(request), "등록 성공");
    }

    @Operation(summary = "코드 상세 삭제")
    @DeleteMapping("/details/{codeDetailId}")
    public ResponseEntity<?> removeCodeDetail(@PathVariable Long codeDetailId) {
        codeService.removeCodeDetail(codeDetailId);
        return ResponseUtil.noContent();
    }
}
