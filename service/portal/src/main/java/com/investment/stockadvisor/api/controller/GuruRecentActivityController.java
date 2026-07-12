package com.investment.stockadvisor.api.controller;

import com.investment.stockadvisor.application.dto.guru.GuruRecentActivitySearchRequest;
import com.investment.stockadvisor.application.service.guru.GuruRecentActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.application.dto.RokResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "구루 매매 활동", description = "저명 투자자(구루) 최근 매매 활동 내역 API")
@RestController
@RequestMapping("/api/v1/guru/activities")
@RequiredArgsConstructor
public class GuruRecentActivityController {

    private final GuruRecentActivityService guruRecentActivityService;

    @Operation(summary = "구루 매매 활동 목록 조회", description = "저명 투자자의 최근 매매 활동 내역 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("")
    public ResponseEntity<?> findGuruRecentActivityAll(GuruRecentActivitySearchRequest searchRequest) {
        return ResponseUtil.success(guruRecentActivityService.findGuruRecentActivityAll(searchRequest));
    }

    @Operation(summary = "구루 매매 활동 단건 조회", description = "ID로 구루 매매 활동 항목을 단건 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = RokResponse.class))),
        @ApiResponse(responseCode = "404", description = "데이터 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> findGuruRecentActivityById(@PathVariable Long id) {
        return ResponseUtil.success(guruRecentActivityService.findGuruRecentActivityById(id));
    }
}
