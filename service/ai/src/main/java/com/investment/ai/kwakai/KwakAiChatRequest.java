package com.investment.ai.kwakai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "KwakAI 채팅 요청")
public class KwakAiChatRequest {

    @Schema(description = "사용할 모델명 (비워두면 기본 모델 사용)")
    private String model;

    @NotNull
    @Schema(description = "대화 메시지 목록")
    private List<KwakAiMessage> messages;
}
