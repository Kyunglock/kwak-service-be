package com.investment.portal.application.service.qa;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이 테스트는 FortuneServiceTest/TradeCaptureServiceTest 에서 실제로 겪은 flaky 버그
 * (클래스 로드 시점에 LocalDate.now() 를 static final 로 고정해 KST 자정을 걸치면
 * 깨짐)를 되풀이하지 않는다 — "오늘"은 각 테스트 메서드 안에서 그때그때 다시 구한다.
 */
class PeriodHintResolverTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PeriodHintResolver resolver = new PeriodHintResolver();

    @Test
    void 올해는_올해_1월1일부터_오늘까지() {
        LocalDate today = LocalDate.now(KST);

        PeriodHintResolver.DateRange range = resolver.resolve("올해");

        assertThat(range.start()).isEqualTo(LocalDate.of(today.getYear(), 1, 1));
        assertThat(range.end()).isEqualTo(today);
    }

    @Test
    void 작년은_작년_1월1일부터_12월31일까지() {
        LocalDate today = LocalDate.now(KST);
        int lastYear = today.getYear() - 1;

        PeriodHintResolver.DateRange range = resolver.resolve("작년");

        assertThat(range.start()).isEqualTo(LocalDate.of(lastYear, 1, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(lastYear, 12, 31));
    }

    @Test
    void 최근_N개월() {
        LocalDate today = LocalDate.now(KST);

        PeriodHintResolver.DateRange range = resolver.resolve("최근 3개월");

        assertThat(range.start()).isEqualTo(today.minusMonths(3));
        assertThat(range.end()).isEqualTo(today);
    }

    @Test
    void 최근_N일() {
        LocalDate today = LocalDate.now(KST);

        PeriodHintResolver.DateRange range = resolver.resolve("최근 30일");

        assertThat(range.start()).isEqualTo(today.minusDays(30));
        assertThat(range.end()).isEqualTo(today);
    }

    @Test
    void 최근_N년() {
        LocalDate today = LocalDate.now(KST);

        PeriodHintResolver.DateRange range = resolver.resolve("최근 2년");

        assertThat(range.start()).isEqualTo(today.minusYears(2));
        assertThat(range.end()).isEqualTo(today);
    }

    @Test
    void null이거나_알아듣지_못하면_최근_1년_기본값() {
        LocalDate today = LocalDate.now(KST);

        assertThat(resolver.resolve(null).start()).isEqualTo(today.minusYears(1));
        assertThat(resolver.resolve("").start()).isEqualTo(today.minusYears(1));
        assertThat(resolver.resolve("지난 분기").start()).isEqualTo(today.minusYears(1));
    }
}
