package com.investment.portal.application.service.insight;

import org.springframework.stereotype.Component;

@Component
public class CombinedInsightPromptBuilder {

    public static final String SYSTEM_PROMPT =
            "당신은 한국어로 응답하는 주식 포트폴리오 분석 전문가입니다. " +
            "반드시 유효한 JSON 객체 하나만 출력하세요. 코드펜스(```)나 설명 문장을 절대 포함하지 마세요. " +
            "모든 텍스트는 한국어로 작성하고 한자(漢字)/중국어/일본어를 사용하지 마세요. " +
            "JSON 스키마: " +
            "{\"profile_fit\":{\"fit\":[{\"ticker\":\"종목명\",\"level\":\"높음|보통|낮음\",\"reason\":\"한 줄 근거\"}]," +
            "\"rebalance\":[\"제안1\",\"제안2\"]}," +
            "\"risk_assessment\":[\"문장1\"],\"portfolio_alignment\":[\"문장1\"],\"investment_recommendation\":[\"문장1\"]," +
            "\"dividend_insight\":{\"summary\":\"배당 총평 한 문단\",\"profileContrast\":\"성향-배당 대조 한 문단\",\"findings\":[\"발견1\"]}}";

    public String build(InsightPromptContext ctx) {
        return String.format(
                "[포트폴리오 종합 분석 요청]\n" +
                "투자자 성향:\n%s\n\n" +
                "포트폴리오 요약: 종목 %d개 / 섹터 %d개\n" +
                "%s\n" +
                "%s\n" +
                "보유 종목:\n%s\n\n" +
                "위 데이터를 바탕으로 다음 5가지를 분석하여 지정된 JSON 스키마로만 응답하세요.\n" +
                "1) profile_fit: 보유 종목별 투자성향 적합도(level=높음/보통/낮음, reason 한 줄)와 맞춤 리밸런싱 제안 2~4개\n" +
                "2) risk_assessment: 포트폴리오 리스크 평가 4~6문장\n" +
                "3) portfolio_alignment: 투자 성향과 포트폴리오의 정합성 4~6문장\n" +
                "4) investment_recommendation: 맞춤 투자 추천 4~6문장\n" +
                "5) dividend_insight: 배당 컨텍스트 기반 총평(summary), 성향-배당 대조(profileContrast), 발견사항(findings 2~4개). " +
                "수치를 새로 계산하지 말고 제공된 배당 컨텍스트의 수치만 인용하세요.",
                ctx.surveyBlock(), ctx.itemCount(), ctx.sectorCount(),
                ctx.metricsBlock(), ctx.dividendBlock(), ctx.stockLines());
    }
}
