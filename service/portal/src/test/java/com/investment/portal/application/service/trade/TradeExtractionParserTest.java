package com.investment.portal.application.service.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeExtractionParserTest {

    private final TradeExtractionParser parser = new TradeExtractionParser(new ObjectMapper());

    @Test
    void 정상_JSON은_그대로_추출된다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":10,
                            "price":230.15,"amount":null,"date":"2026-09-18","currency":"USD"}]}
                """).trades();

        assertThat(trades).hasSize(1);
        ExtractedTrade t = trades.get(0);
        assertThat(t.name()).isEqualTo("애플");
        assertThat(t.ticker()).isEqualTo("AAPL");
        assertThat(t.qty()).isEqualByComparingTo("10");
        assertThat(t.price()).isEqualByComparingTo("230.15");
        assertThat(t.date()).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void 마크다운_코드블록으로_감싸도_파싱된다() {
        List<ExtractedTrade> trades = parser.parse("""
                ```json
                {"trades":[{"name":"삼성전자","type":"BUY","qty":5,"price":71000,"currency":"KRW"}]}
                ```
                """).trades();

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).name()).isEqualTo("삼성전자");
        assertThat(trades.get(0).currency()).isEqualTo("KRW");
    }

    @Test
    void 쉼표와_통화기호가_섞인_숫자도_읽는다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[{"name":"엔비디아","type":"BUY","qty":"1,200","price":"$1,234.56"}]}
                """).trades();

        assertThat(trades.get(0).qty()).isEqualByComparingTo("1200");
        assertThat(trades.get(0).price()).isEqualByComparingTo("1234.56");
    }

    @Test
    void 단가가_없고_총액과_수량이_있으면_단가를_역산한다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":4,"price":null,"amount":1000}]}
                """).trades();

        assertThat(trades.get(0).price()).isEqualByComparingTo("250");
    }

    @Test
    void 수량이_0이면_단가를_역산하지_않는다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":0,"price":null,"amount":1000}]}
                """).trades();

        assertThat(trades.get(0).price()).isNull();
    }

    @Test
    void 깨진_줄은_건너뛰고_나머지는_살린다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[
                  {"ticker":null,"name":null,"type":"BUY","qty":1,"price":1},
                  {"name":"애플","type":"BUY","qty":2,"price":100}
                ]}
                """).trades();

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).name()).isEqualTo("애플");
    }

    @Test
    void 문자열_null과_NA는_null로_취급된다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[{"name":"애플","ticker":"null","type":"BUY","qty":"N/A","price":"-","date":"null"}]}
                """).trades();

        ExtractedTrade t = trades.get(0);
        assertThat(t.ticker()).isNull();
        assertThat(t.qty()).isNull();
        assertThat(t.price()).isNull();
        assertThat(t.date()).isNull();
    }

    @Test
    void 매도_표현은_SELL로_정규화된다() {
        assertThat(parser.parse("""
                {"trades":[{"name":"애플","type":"매도","qty":1,"price":1}]}
                """).trades().get(0).type()).isEqualTo("SELL");

        assertThat(parser.parse("""
                {"trades":[{"name":"애플","type":"sell","qty":1,"price":1}]}
                """).trades().get(0).type()).isEqualTo("SELL");
    }

    @Test
    void 거래유형이_불명확하면_BUY로_둔다() {
        assertThat(parser.parse("""
                {"trades":[{"name":"애플","type":null,"qty":1,"price":1}]}
                """).trades().get(0).type()).isEqualTo("BUY");
    }

    @Test
    void 여러_날짜_형식을_지원한다() {
        assertThat(parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":1,"price":1,"date":"2026/09/18"}]}
                """).trades().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 18));

        assertThat(parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":1,"price":1,"date":"20260918"}]}
                """).trades().get(0).date()).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void 통화는_원화_달러_표현도_인식한다() {
        assertThat(parser.parse("""
                {"trades":[{"name":"삼성전자","type":"BUY","qty":1,"price":1,"currency":"원"}]}
                """).trades().get(0).currency()).isEqualTo("KRW");

        assertThat(parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":1,"price":1,"currency":"달러"}]}
                """).trades().get(0).currency()).isEqualTo("USD");
    }

    @Test
    void JSON이_아니거나_trades가_없으면_빈_목록() {
        assertThat(parser.parse(null).trades()).isEmpty();
        assertThat(parser.parse("").trades()).isEmpty();
        assertThat(parser.parse("죄송합니다, 매매 내역을 찾지 못했습니다").trades()).isEmpty();
        assertThat(parser.parse("{\"result\":\"없음\"}").trades()).isEmpty();
        assertThat(parser.parse("{망가진 json").trades()).isEmpty();
    }

    @Test
    void 추출_건수는_상한에서_잘린다() {
        StringBuilder sb = new StringBuilder("{\"trades\":[");
        for (int i = 0; i < 80; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"name\":\"애플\",\"type\":\"BUY\",\"qty\":1,\"price\":1}");
        }
        sb.append("]}");

        assertThat(parser.parse(sb.toString()).trades()).hasSize(50);
    }

    @Test
    void 총액만_있고_수량이_없으면_단가는_null로_남는다() {
        List<ExtractedTrade> trades = parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":null,"price":null,"amount":1000}]}
                """).trades();

        assertThat(trades.get(0).price()).isNull();
        assertThat(trades.get(0).amount()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    // ── 화면 종류 ────────────────────────────────────────────────────────────────

    @Test
    void 잔고_화면으로_판정하면_그대로_전달된다() {
        TradeExtraction extraction = parser.parse("""
                {"screenType":"BALANCE","trades":[
                  {"name":"알파벳 C","ticker":"GOOG","type":"BUY","qty":9,
                   "price":343.0188,"amount":3087.17,"date":null,"currency":null}
                ]}
                """);

        assertThat(extraction.isBalanceScreen()).isTrue();
        assertThat(extraction.trades()).hasSize(1);
        assertThat(extraction.trades().get(0).ticker()).isEqualTo("GOOG");
        assertThat(extraction.trades().get(0).amount()).isEqualByComparingTo("3087.17");
    }

    @Test
    void screenType이_없으면_체결내역으로_본다() {
        TradeExtraction extraction = parser.parse("""
                {"trades":[{"name":"애플","type":"BUY","qty":1,"price":1}]}
                """);

        assertThat(extraction.screenType()).isEqualTo(TradeExtraction.EXECUTION);
        assertThat(extraction.isBalanceScreen()).isFalse();
    }

    @Test
    void 모르는_screenType은_체결내역으로_떨어뜨린다() {
        assertThat(parser.parse("""
                {"screenType":"뭔가다른것","trades":[{"name":"애플","type":"BUY","qty":1,"price":1}]}
                """).screenType()).isEqualTo(TradeExtraction.EXECUTION);
    }

    // ── 형식이 어긋난 응답 ───────────────────────────────────────────────────────

    @Test
    void 추론_블록이_앞에_붙어도_본문_JSON을_집어낸다() {
        // 예전에는 첫 '{' 부터 마지막 '}' 까지를 통째로 잘라 JSON 전체가 깨졌다
        TradeExtraction extraction = parser.parse("""
                <think>표를 보니 {행이 4개} 있고 마지막 줄은 잘렸다</think>
                다음은 결과입니다:
                {"screenType":"BALANCE","trades":[{"name":"모더나","ticker":"MRNA","type":"BUY","qty":1,"price":161.31}]}
                """);

        assertThat(extraction.trades()).hasSize(1);
        assertThat(extraction.trades().get(0).ticker()).isEqualTo("MRNA");
        assertThat(extraction.isBalanceScreen()).isTrue();
    }

    @Test
    void 설명_객체가_먼저_나와도_trades가_든_객체를_고른다() {
        TradeExtraction extraction = parser.parse("""
                {"note":"화면이 흐립니다"}
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":2,"price":100}]}
                """);

        assertThat(extraction.trades()).hasSize(1);
        assertThat(extraction.trades().get(0).ticker()).isEqualTo("AAPL");
    }

    @Test
    void 중간에_잘린_응답은_빈_결과로_끝난다() {
        TradeExtraction extraction = parser.parse("""
                {"trades":[{"name":"애플","ticker":"AAPL","type":"BUY","qty":2,
                """);

        assertThat(extraction.trades()).isEmpty();
    }

    @Test
    void 중괄호가_든_문자열이_있어도_객체_끝을_제대로_찾는다() {
        TradeExtraction extraction = parser.parse("""
                {"trades":[{"name":"애플 {특별계좌}","type":"BUY","qty":1,"price":10}]} 이상입니다.
                """);

        assertThat(extraction.trades()).hasSize(1);
        assertThat(extraction.trades().get(0).name()).isEqualTo("애플 {특별계좌}");
    }
}
