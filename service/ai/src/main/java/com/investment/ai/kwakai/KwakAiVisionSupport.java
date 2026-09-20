package com.investment.ai.kwakai;

import com.investment.ai.api.dto.VisionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 호환 chat/completions 의 멀티모달 content 배열을 만든다.
 *
 * <p>kwakai(vLLM)는 OpenAI 호환 API라 이미지도 같은 {@code image_url} 파트 형식으로 받는다.
 * 다만 <b>모델 자체가 멀티모달이어야</b> 한다 — 텍스트 전용 모델에 이미지를 보내면 서버가 거절한다.
 */
final class KwakAiVisionSupport {

    private KwakAiVisionSupport() {}

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
                        "image_url", Map.of("url", "data:" + mime + ";base64," + image.base64())));
            }
        }
        return parts;
    }
}
