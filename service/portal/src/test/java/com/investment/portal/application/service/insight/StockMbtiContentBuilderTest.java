package com.investment.portal.application.service.insight;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class StockMbtiContentBuilderTest {

    private Map<String, Double> fullScores(double v) {
        Map<String, Double> m = new HashMap<>();
        for (String k : new String[]{"EI","SN","TF","JP","수익추구","리스크허용","장기투자","분산투자"}) m.put(k, v);
        return m;
    }

    @Test
    void 전축_고득점이면_ESTJ_GRLD() {
        String[] lines = StockMbtiContentBuilder.build(fullScores(90)).split("\n");
        assertThat(lines[0]).isEqualTo("V2");
        assertThat(lines[1]).isEqualTo("ESTJ");
        assertThat(lines[2]).isEqualTo("엄격한 관리자");
        assertThat(lines[3]).isEqualTo("GRLD");
        assertThat(lines[6]).isEqualTo("90:90:90:90");
        assertThat(lines[7]).isEqualTo("90:90:90:90");
    }

    @Test
    void 전축_저득점이면_INFP_VSTF() {
        String[] lines = StockMbtiContentBuilder.build(fullScores(30)).split("\n");
        assertThat(lines[1]).isEqualTo("INFP");
        assertThat(lines[3]).isEqualTo("VSTF");
    }

    @Test
    void 컷_경계_62_5는_앞글자() {
        String[] lines = StockMbtiContentBuilder.build(fullScores(62.5)).split("\n");
        assertThat(lines[1]).isEqualTo("ESTJ");
        assertThat(lines[3]).isEqualTo("GRLD");
        // 62.5 미만은 뒷글자
        String[] below = StockMbtiContentBuilder.build(fullScores(62.4)).split("\n");
        assertThat(below[1]).isEqualTo("INFP");
        assertThat(below[3]).isEqualTo("VSTF");
    }

    @Test
    void 성격축이_하나라도_없으면_NONE_점수는_0() {
        Map<String, Double> m = fullScores(80);
        m.remove("JP"); // 구응답자: 성격 문항 미응답
        String[] lines = StockMbtiContentBuilder.build(m).split("\n");
        assertThat(lines[1]).isEqualTo("NONE");
        assertThat(lines[2]).isEqualTo("-");
        assertThat(lines[3]).isEqualTo("GRLD"); // 투자 파트는 정상
        assertThat(lines[6]).isEqualTo("0:0:0:0");
    }

    @Test
    void 투자_16유형_전부_메타가_있다() {
        for (String g : new String[]{"G","V"})
            for (String r : new String[]{"R","S"})
                for (String l : new String[]{"L","T"})
                    for (String d : new String[]{"D","F"}) {
                        Map<String, Double> m = fullScores(80);
                        m.put("수익추구", g.equals("G") ? 80.0 : 30.0);
                        m.put("리스크허용", r.equals("R") ? 80.0 : 30.0);
                        m.put("장기투자", l.equals("L") ? 80.0 : 30.0);
                        m.put("분산투자", d.equals("D") ? 80.0 : 30.0);
                        String[] lines = StockMbtiContentBuilder.build(m).split("\n");
                        assertThat(lines[3]).isEqualTo(g + r + l + d);
                        assertThat(lines[4]).as(lines[3]).isNotBlank().doesNotContain("분석 중");
                        assertThat(lines[5]).as(lines[3]).isNotBlank();
                    }
    }

    @Test
    void 빈_맵이면_투자축은_기본값_50으로_VSTF() {
        // buildStockMbti 호출부가 빈 결과는 MISSING_CONTENT로 분기하므로 build()에 빈 맵이 오는 일은 없지만,
        // 방어적으로 기본값 50(<62.5) → 전부 뒷글자
        String[] lines = StockMbtiContentBuilder.build(new HashMap<>()).split("\n");
        assertThat(lines[1]).isEqualTo("NONE");
        assertThat(lines[3]).isEqualTo("VSTF");
    }
}
