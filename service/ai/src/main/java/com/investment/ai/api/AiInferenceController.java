package com.investment.ai.api;

import com.investment.ai.api.dto.*;
import com.investment.ai.kwakai.KwakAiClient;
import com.investment.ai.openai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiInferenceController {

    private final KwakAiClient kwakAiClient;
    private final OpenAiClient openAiClient;

    @PostMapping("/kwakai/generate")
    public GenerateResponse kwakaiGenerate(@RequestBody GenerateRequest req) {
        return new GenerateResponse(kwakAiClient.generateContent(req.system(), req.user()));
    }

    @PostMapping("/openai/chat")
    public ChatResponse openaiChat(@RequestBody GenerateRequest req) {
        OpenAiClient.ChatResponse r = openAiClient.chat(req.system(), req.user());
        return new ChatResponse(r.content(), r.promptTokens(), r.completionTokens());
    }

    /** 이미지 + 텍스트 추론 (증권사 앱 스크린샷에서 매매내역 추출 등). */
    @PostMapping("/openai/vision")
    public ChatResponse openaiVision(@RequestBody VisionRequest req) {
        OpenAiClient.ChatResponse r = openAiClient.vision(req.system(), req.user(), req.images());
        return new ChatResponse(r.content(), r.promptTokens(), r.completionTokens());
    }
}
