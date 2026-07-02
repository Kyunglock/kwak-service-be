package com.investment.ai.kwakai;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "KwakAI", description = "로컬 LLM 외부 접근 프록시 API")
@RestController
@RequestMapping("/api/v1/kwakai")
@RequiredArgsConstructor
public class KwakAiController {

    private final KwakAiClient kwakAiClient;

    @Operation(summary = "채팅 완성", description = "messages 배열로 대화 맥락을 전달하세요.")
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody KwakAiChatRequest request) {
        JsonNode result = kwakAiClient.chat(request);
        return ResponseUtil.success(result, "채팅 완성 성공");
    }

    @Operation(summary = "텍스트 생성", description = "단일 프롬프트 기반 생성에 사용하세요.")
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody KwakAiGenerateRequest request) {
        JsonNode result = kwakAiClient.generate(request);
        return ResponseUtil.success(result, "텍스트 생성 성공");
    }

    @Operation(summary = "사용 가능한 모델 목록 조회")
    @GetMapping("/models")
    public ResponseEntity<?> listModels() {
        JsonNode result = kwakAiClient.listModels();
        return ResponseUtil.success(result, "모델 목록 조회 성공");
    }
}
