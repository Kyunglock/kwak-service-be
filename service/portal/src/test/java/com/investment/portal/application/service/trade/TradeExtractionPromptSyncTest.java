package com.investment.portal.application.service.trade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프롬프트는 두 곳에 있다 — n8n 워크플로우(운영 경로)와 {@link TradeExtractionPrompt}(폴백 경로).
 * 한쪽만 고치면 TRADE_EXTRACTION_VIA_N8N 을 켜고 끄는 것만으로 추출 결과가 달라진다.
 *
 * <p>두 프롬프트는 형식이 조금 다르다(n8n 은 JS 템플릿 리터럴이라 백틱을 못 쓰고, 응답 예시도
 * 한 줄로 적는다). 그래서 통째로 비교하지 않고, "이게 빠지면 기능이 망가지는" 규칙만 골라
 * 양쪽에 다 있는지 확인한다. 특히 잔고 화면 관련 규칙이 빠지면 보유종목 캡처가 통째로
 * 빈 배열로 돌아온다 — 이미지를 못 읽어서가 아니라 모델이 규칙대로 버려서.
 */
class TradeExtractionPromptSyncTest {

    private static final Path WORKFLOW = Path.of("n8n", "workflows", "trade-extract.json");

    /** 양쪽 프롬프트에 그대로 들어 있어야 하는 규칙들. 워크플로우가 JSON 이라 한 줄짜리만 넣는다. */
    private static final List<String> CORE_RULES = List.of(
            "screenType",
            "BALANCE",
            "체결 시점이 안 적혀 있다는 이유로 빈 배열을 돌려주면 안 됩니다.",
            "매입가·평단가 (현재가·시장가가 아닙니다)",
            "qty × price 와 대조해 검산하는 데 씁니다",
            "표 바깥의 숫자는 절대 거래로 만들지 마세요",
            "두 줄을 합쳐 한 건으로 만드세요",
            "GOOG 와 GOOGL 은 다른 종목입니다");

    @Test
    void n8n_워크플로우와_자바_프롬프트가_같은_규칙을_담고_있다() throws IOException {
        String workflow = Files.readString(repoFile(WORKFLOW), StandardCharsets.UTF_8);
        String java = TradeExtractionPrompt.SYSTEM + TradeExtractionPrompt.IMAGE_USER;

        for (String rule : CORE_RULES) {
            assertThat(java)
                    .as("TradeExtractionPrompt 에 빠진 규칙: %s", rule)
                    .contains(rule);
            assertThat(workflow)
                    .as("n8n trade-extract 워크플로우에 빠진 규칙: %s "
                            + "(양쪽 프롬프트를 같이 고쳐야 한다)", rule)
                    .contains(rule);
        }
    }

    /** 테스트 작업 디렉터리는 모듈(service/portal)이라 저장소 루트까지 거슬러 올라간다. */
    private Path repoFile(Path relative) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(relative);
            if (Files.exists(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("저장소에서 " + relative + " 를 찾지 못했습니다");
    }
}
