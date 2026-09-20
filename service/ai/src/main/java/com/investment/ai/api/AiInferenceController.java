package com.investment.ai.api;

import com.investment.ai.api.dto.*;
import com.investment.ai.kwakai.KwakAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * core(portal) → ai 추론 게이트웨이.
 *
 * <p>모든 추론은 로컬 LLM(kwakai)으로 처리한다. 외부 LLM 벤더 연동은 두지 않는다.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiInferenceController {

    private final KwakAiClient kwakAiClient;

    /** 단순 텍스트 생성. 실패 시 content 가 null 로 온다. */
    @PostMapping("/kwakai/generate")
    public GenerateResponse kwakaiGenerate(@RequestBody GenerateRequest req) {
        return new GenerateResponse(kwakAiClient.generateContent(req.system(), req.user()));
    }

    /** 구조화 응답(JSON)을 기대하는 호출. 토큰 수 포함. */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody GenerateRequest req) {
        KwakAiClient.ChatResult r = kwakAiClient.chat(req.system(), req.user());
        return new ChatResponse(r.content(), r.promptTokens(), r.completionTokens());
    }

    /** 이미지 + 텍스트 추론 (증권사 앱 스크린샷에서 매매내역 추출 등). */
    @PostMapping("/vision")
    public ChatResponse vision(@RequestBody VisionRequest req) {
        KwakAiClient.ChatResult r = kwakAiClient.vision(req.system(), req.user(), req.images());
        return new ChatResponse(r.content(), r.promptTokens(), r.completionTokens());
    }
}
