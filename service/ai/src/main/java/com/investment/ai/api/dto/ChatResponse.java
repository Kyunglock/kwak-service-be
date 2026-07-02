package com.investment.ai.api.dto;
public record ChatResponse(String content, int promptTokens, int completionTokens) {}
