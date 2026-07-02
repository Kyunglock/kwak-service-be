package com.investment.stockadvisor.application.service.divergence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.stockadvisor.application.dto.divergence.DivergenceInterpretationResponse;
import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;
import com.investment.stockadvisor.domain.repository.divergence.DivergenceDetectionResultMapper;
import kwak.common.ai.AiGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DivergenceInterpretationServiceImpl implements DivergenceInterpretationService {

    private static final String CACHE_PREFIX = "divergence:interpretation:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private static final String SYSTEM_PROMPT = """
            You are a financial analyst specializing in equity accounting quality and earnings divergence.
            Analyze the provided divergence detection result and respond ONLY with a valid JSON object:
            {
              "summary": "2-3 sentence explanation of the detected anomaly and its investment significance",
              "risk_level": "HIGH or MEDIUM or LOW",
              "key_drivers": ["driver 1", "driver 2"],
              "watch_points": ["what to monitor 1", "what to monitor 2"]
            }
            """;

    private final DivergenceDetectionResultMapper resultMapper;
    private final AiGatewayClient aiGatewayClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public DivergenceInterpretationResponse interpret(DivergenceDetectionResult result) {
        String cacheKey = buildCacheKey(result);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);
        }

        try {
            AiGatewayClient.ChatResponse chatResponse = aiGatewayClient.openaiChat(SYSTEM_PROMPT, buildUserPrompt(result));
            log.info("[OpenAI] stockCd={} type={} prompt_tokens={} completion_tokens={}",
                    result.getStockCd(), result.getDivergenceType(),
                    chatResponse.promptTokens(), chatResponse.completionTokens());

            DivergenceInterpretationResponse response = parseResponse(chatResponse.content(), result, false);
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL);
            return response;
        } catch (Exception e) {
            log.error("[OpenAI] interpretation failed stockCd={} type={}: {}",
                    result.getStockCd(), result.getDivergenceType(), e.getMessage());
            throw new RuntimeException("Failed to interpret divergence result", e);
        }
    }

    @Override
    public List<DivergenceInterpretationResponse> interpretByStockCd(String stockCd) {
        return resultMapper.findByStockCd(stockCd).stream()
                .map(this::interpret)
                .collect(Collectors.toList());
    }

    private String buildCacheKey(DivergenceDetectionResult result) {
        return CACHE_PREFIX
                + result.getStockCd() + ":"
                + result.getDivergenceType() + ":"
                + result.getFiscalYear() + ":"
                + result.getFiscalQuarter();
    }

    private String buildUserPrompt(DivergenceDetectionResult result) {
        return String.format(
                "Stock: %s%nDivergence Type: %s%nPeriod: %dQ%d%nSeverity: %.4f (0=minimal, 1=extreme)%nEvidence: %s",
                result.getStockCd(),
                result.getDivergenceType(),
                result.getFiscalYear(),
                result.getFiscalQuarter(),
                result.getSeverity(),
                result.getEvidence()
        );
    }

    @SuppressWarnings("unchecked")
    private DivergenceInterpretationResponse parseResponse(
            String content, DivergenceDetectionResult result, boolean cached) throws Exception {
        Map<String, Object> parsed = objectMapper.readValue(content, Map.class);
        return new DivergenceInterpretationResponse(
                result.getStockCd(),
                result.getDivergenceType(),
                result.getFiscalYear(),
                result.getFiscalQuarter(),
                (String) parsed.get("summary"),
                (String) parsed.get("risk_level"),
                (List<String>) parsed.get("key_drivers"),
                (List<String>) parsed.get("watch_points"),
                cached
        );
    }

    private DivergenceInterpretationResponse deserialize(String json) {
        try {
            DivergenceInterpretationResponse r = objectMapper.readValue(json, DivergenceInterpretationResponse.class);
            return new DivergenceInterpretationResponse(
                    r.stockCd(), r.divergenceType(), r.fiscalYear(), r.fiscalQuarter(),
                    r.summary(), r.riskLevel(), r.keyDrivers(), r.watchPoints(), true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize cached interpretation", e);
        }
    }
}
