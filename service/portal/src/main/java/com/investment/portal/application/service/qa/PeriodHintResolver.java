package com.investment.portal.application.service.qa;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "올해", "최근 3개월" 같은 자유 표현을 날짜 범위로 바꾼다.
 *
 * <p>LLM은 원문 표현을 그대로 옮기기만 하고(QuestionIntentExtractionPrompt), 실제
 * 날짜 산술은 여기서 결정적으로 한다 — 로컬 LLM에게 날짜 계산을 맡기지 않는다는
 * TradeExtractionPrompt 의 원칙을 그대로 잇는다.
 */
@Component
public class PeriodHintResolver {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final Pattern YEARS = Pattern.compile("최근\\s*(\\d+)\\s*년");
    private static final Pattern MONTHS = Pattern.compile("최근\\s*(\\d+)\\s*개월");
    private static final Pattern DAYS = Pattern.compile("최근\\s*(\\d+)\\s*일");

    public record DateRange(LocalDate start, LocalDate end) {}

    /** 알아듣지 못한 표현은 최근 1년으로 넓게 잡는다 — "모르면 null"이 아니라
     * "모르면 넓게"인 이유는, 이 값은 사용자에게 보여줄 응답이 아니라 쿼리 범위라서
     * 아예 검색을 포기하는 것보다 넓게라도 찾아보는 편이 낫기 때문이다. */
    public DateRange resolve(String hint) {
        LocalDate today = LocalDate.now(KST);
        if (hint == null || hint.isBlank()) {
            return new DateRange(today.minusYears(1), today);
        }
        String h = hint.trim();

        if (h.contains("올해")) {
            return new DateRange(LocalDate.of(today.getYear(), 1, 1), today);
        }
        if (h.contains("작년")) {
            int year = today.getYear() - 1;
            return new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        }

        Matcher m;
        if ((m = MONTHS.matcher(h)).find()) {
            return new DateRange(today.minusMonths(Long.parseLong(m.group(1))), today);
        }
        if ((m = DAYS.matcher(h)).find()) {
            return new DateRange(today.minusDays(Long.parseLong(m.group(1))), today);
        }
        if ((m = YEARS.matcher(h)).find()) {
            return new DateRange(today.minusYears(Long.parseLong(m.group(1))), today);
        }

        return new DateRange(today.minusYears(1), today);
    }
}
