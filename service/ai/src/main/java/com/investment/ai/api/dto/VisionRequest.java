package com.investment.ai.api.dto;

import java.util.List;

/**
 * 이미지 + 텍스트 추론 요청.
 *
 * <p>images 의 base64 는 순수 base64 문자열이며 {@code data:} 접두어를 포함하지 않는다.
 * data URL 조립은 KwakAiVisionSupport 가 담당한다.
 */
public record VisionRequest(String system, String user, List<ImagePart> images) {

    public record ImagePart(String mimeType, String base64) {}
}
