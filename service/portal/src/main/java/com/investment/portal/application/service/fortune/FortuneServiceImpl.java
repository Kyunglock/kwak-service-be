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
import java.util.Optional;
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
            - 운세는 늘 좋지만은 않습니다. 기운이 탁한 날, 구름이 낀 날, 조심스러운 날도
              같은 확률로 자신 있게 말하세요. 행운 지수는 1~5 전 범위를 골고루 사용하세요 (5점 남발 금지).
            - 절대 금지: 목표 주가 제시, 매수/매도/보유 지시, 실제 투자 판단으로 오해될 문장.
              나쁜 운세도 "팔아라/사지 마라"가 아니라 기운·분위기 묘사로만 표현하세요.
            - "이번 주", "이번 달", "올해" 등 하루보다 긴 시점 표현 금지. "오늘"은 사용 가능.
            - 한국어 3~5문장, 전체 300자 내외, 이모지를 적극 사용하세요.
            - 마지막 줄은 반드시 다음 형식: 🍀 행운 지수: N/5 | 행운의 요일: ○요일 | 행운의 색: ○○
            """;

    private final FortuneMapper fortuneMapper;
    private final AiGatewayClient aiGatewayClient;

    @Override
    public FortuneResponse getFortune(String rawTicker) {
        String input = rawTicker == null ? "" : rawTicker.trim();
        if (input.isEmpty() || input.length() > 30) {
            throw new UnsupportedTickerException();
        }
        String upper = input.toUpperCase();
        // 티커 형식이면 티커 조회 먼저, 미스면 종목명 폴백 (NAVER처럼 영문 종목명이 티커 형식과 겹치는 케이스 흡수)
        String ticker = (TICKER_PATTERN.matcher(upper).matches()
                        ? fortuneMapper.findCanonicalTicker(upper)
                        : Optional.<String>empty())
                .or(() -> fortuneMapper.findCanonicalTickerByName(upper))
                .orElseThrow(UnsupportedTickerException::new);

        LocalDate today = LocalDate.now(KST);
        return fortuneMapper.findByTickerAndDate(ticker, today)
                .map(FortuneResponse::from)
                .orElseGet(() -> generateAndSave(ticker, today));
    }

    // 주의: 이 서비스에 @Transactional을 붙이면 안 된다.
    // DuplicateKeyException 후 재SELECT는 auto-commit(문장별 새 스냅숏) 전제 —
    // 트랜잭션으로 묶으면 REPEATABLE_READ 스냅숏이 경쟁자의 커밋 행을 못 봐 빈 결과가 된다.
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
