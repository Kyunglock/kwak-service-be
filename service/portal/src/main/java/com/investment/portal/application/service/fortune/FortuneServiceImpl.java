package com.investment.portal.application.service.fortune;

import com.investment.portal.application.dto.fortune.FortuneResponse;
import com.investment.portal.domain.entity.fortune.StockFortune;
import com.investment.portal.domain.repository.fortune.FortuneMapper;
import kwak.common.ai.AiGatewayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FortuneServiceImpl implements FortuneService {

    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Z0-9.\\-]{1,20}$");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 톤이 기능의 전부 — 비과학적 근거를 자신감 있게, 단 투자 조언으로 오해될 문장은 금지
    private static final String SYSTEM_PROMPT = """
            당신은 "주식 점술가"입니다. 종목의 기운을 읽어 재미로 보는 오늘의 운세를 알려줍니다.
            - 별자리, 사주, 풍수, 숫자 궁합, 티커 글자의 획수, 로고 색상, 창립자의 기운 같은
              비과학적 근거를 자신감 있게 섞어 그럴듯하게 말하세요.
            - 절대 금지: 목표 주가 제시, 매수/매도/보유 지시, 실제 투자 판단으로 오해될 문장.
            - "이번 주", "이번 달", "올해" 등 하루보다 긴 시점 표현 금지. "오늘"은 사용 가능.
            - 한국어 3~5문장, 전체 300자 내외, 이모지를 적극 사용하세요.
            - 마지막 줄은 반드시 다음 형식: 🍀 행운 지수: N/5 | 행운의 요일: ○요일 | 행운의 색: ○○
            """;

    private final FortuneMapper fortuneMapper;
    private final AiGatewayClient aiGatewayClient;

    @Override
    public FortuneResponse getFortune(String rawTicker) {
        String input = rawTicker == null ? "" : rawTicker.trim().toUpperCase();
        if (!TICKER_PATTERN.matcher(input).matches()) {
            throw new UnsupportedTickerException();
        }
        String ticker = fortuneMapper.findCanonicalTicker(input)
                .orElseThrow(UnsupportedTickerException::new);

        LocalDate today = LocalDate.now(KST);
        return fortuneMapper.findByTickerAndDate(ticker, today)
                .map(FortuneResponse::from)
                .orElseGet(() -> generateAndSave(ticker, today));
    }

    private FortuneResponse generateAndSave(String ticker, LocalDate today) {
        String content = callLlm(ticker);
        try {
            fortuneMapper.insert(StockFortune.builder()
                    .ticker(ticker).fortuneDate(today).content(content).build());
        } catch (DuplicateKeyException e) {
            // 동시 미스 경합 — 선점된 운세를 반환 (내 LLM 호출 1회는 버림)
        }
        // INSERT 직후 재SELECT — reg_dt 등 DB 기본값을 채운 저장본으로 응답 통일
        return fortuneMapper.findByTickerAndDate(ticker, today)
                .map(FortuneResponse::from)
                .orElseThrow(FortuneUnavailableException::new);
    }

    private String callLlm(String ticker) {
        final String content;
        try {
            content = aiGatewayClient.generateContent(SYSTEM_PROMPT, "종목: " + ticker);
        } catch (Exception e) {
            throw new FortuneUnavailableException(e);
        }
        // AiGatewayClient는 응답 없으면 null 또는 문자열 "null" 반환 가능
        if (content == null || content.isBlank() || "null".equals(content)) {
            throw new FortuneUnavailableException();
        }
        return content;
    }
}
