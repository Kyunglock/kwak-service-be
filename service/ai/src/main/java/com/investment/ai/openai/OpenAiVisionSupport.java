package com.investment.ai.openai;

import com.investment.ai.api.dto.VisionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** OpenAI chat/completions 의 멀티모달 content 배열을 만든다. */
final class OpenAiVisionSupport {

    private OpenAiVisionSupport() {}

    /**
     * 텍스트 1개 + 이미지 N개를 content 파트 배열로 변환한다.
     * detail=high 는 증권사 앱 스크린샷의 작은 숫자를 읽기 위해 필요하다 (low 로는 수량·단가를 놓친다).
     */
    static List<Map<String, Object>> toContentParts(String userText, List<VisionRequest.ImagePart> images) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", userText == null ? "" : userText));

        if (images != null) {
            for (VisionRequest.ImagePart image : images) {
                if (image == null || image.base64() == null || image.base64().isBlank()) {
                    continue;
                }
                String mime = (image.mimeType() == null || image.mimeType().isBlank())
                        ? "image/png" : image.mimeType();
                parts.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of(
                                "url", "data:" + mime + ";base64," + image.base64(),
                                "detail", "high")));
            }
        }
        return parts;
    }
}
