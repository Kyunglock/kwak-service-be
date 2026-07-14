package com.investment.portal.application.service.insight;

import java.util.Map;
import java.util.Set;

/**
 * STOCK_MBTI content(V2) 산출 — 순수 함수 (LLM/DB 미사용).
 * 포맷(줄 구분): V2 / 성격코드|NONE / 성격별칭|- / 투자코드 / 투자유형명 / 투자유형설명
 *              / EI:SN:TF:JP / 수익:리스크:장기:분산
 * 컷 62.5 = 옵션 점수(25~100)의 실제 중간값. >= 62.5 이면 앞 글자.
 */
final class StockMbtiContentBuilder {

    static final double AXIS_CUT = 62.5;
    static final Set<String> INVEST_AXES = Set.of("수익추구", "리스크허용", "장기투자", "분산투자");

    static final String MISSING_CONTENT =
            "설문 미완료\n투자 MBTI 분석 불가\n설문을 완료하면 나의 MBTI와 투자 MBTI를 확인할 수 있습니다.\n0:0:0";

    private static final Map<String, String> PERSONALITY_ALIAS = Map.ofEntries(
            Map.entry("INTJ", "용의주도한 전략가"), Map.entry("INTP", "논리적인 사색가"),
            Map.entry("ENTJ", "대담한 통솔자"),     Map.entry("ENTP", "뜨거운 논쟁을 즐기는 변론가"),
            Map.entry("INFJ", "선의의 옹호자"),     Map.entry("INFP", "열정적인 중재자"),
            Map.entry("ENFJ", "정의로운 사회운동가"), Map.entry("ENFP", "재기발랄한 활동가"),
            Map.entry("ISTJ", "청렴결백한 논리주의자"), Map.entry("ISFJ", "용감한 수호자"),
            Map.entry("ESTJ", "엄격한 관리자"),     Map.entry("ESFJ", "사교적인 외교관"),
            Map.entry("ISTP", "만능 재주꾼"),       Map.entry("ISFP", "호기심 많은 예술가"),
            Map.entry("ESTP", "모험을 즐기는 사업가"), Map.entry("ESFP", "자유로운 영혼의 연예인"));

    /** {유형명, 설명} — {G/V}{R/S}{L/T}{D/F} 16조합 */
    private static final Map<String, String[]> INVEST_META = Map.ofEntries(
            Map.entry("GRLD", new String[]{"성장 항해사",
                    "높은 수익과 리스크를 감내하며 장기 성장 자산에 투자하되, 여러 자산에 돛을 나눠 다는 유형입니다. 성장 테마 ETF와 다수 종목으로 장기 항해를 이어갑니다."}),
            Map.entry("GRLF", new String[]{"성장 개척자",
                    "확신하는 소수 성장주에 과감하게 집중하고 장기 보유하는 공격적 개척자입니다. 변동에도 원칙을 고수하며 큰 복리 수익을 노립니다."}),
            Map.entry("GRTD", new String[]{"모멘텀 서퍼",
                    "시장의 파도를 여러 종목에 나눠 타며 단기 모멘텀 수익을 챙기는 유형입니다. 분산으로 리스크를 다스리며 기회가 온 파도에 올라탑니다."}),
            Map.entry("GRTF", new String[]{"모멘텀 헌터",
                    "가장 강한 모멘텀 한두 종목에 집중해 단기 수익을 사냥하는 공격적 트레이더입니다. 빠른 진입과 명확한 손절이 무기입니다."}),
            Map.entry("GSLD", new String[]{"균형 설계자",
                    "수익을 추구하되 리스크를 신중히 관리하고, 장기 관점에서 잘 분산된 포트폴리오를 설계하는 유형입니다. 성장과 안정의 황금비를 찾습니다."}),
            Map.entry("GSLF", new String[]{"균형 성장가",
                    "신뢰하는 소수 우량 성장주를 장기 보유하며 수익성과 안정성의 균형을 추구합니다. 검증된 기업에만 집중하는 신중한 성장 투자자입니다."}),
            Map.entry("GSTD", new String[]{"신중한 기회가",
                    "리스크를 낮게 유지하며 단기 기회를 여러 자산에 나눠 담는 계산적인 유형입니다. 이벤트·배당 등 방향성 있는 기회를 분산 공략합니다."}),
            Map.entry("GSTF", new String[]{"신중한 수익가",
                    "수익 목표가 뚜렷하지만 리스크에 민감하며 확실한 소수 기회에 단기 집중합니다. 안전마진을 확보한 뒤에만 움직이는 유형입니다."}),
            Map.entry("VRLD", new String[]{"가치 항해사",
                    "저평가 자산을 여러 곳에서 발굴해 장기 보유하는 분산형 가치 투자자입니다. 시장이 외면한 구간을 넓게 담아 회복을 기다립니다."}),
            Map.entry("VRLF", new String[]{"가치 탐험가",
                    "확신하는 저평가 종목에 집중해 가치가 시세에 반영될 때까지 인내하는 역발상 가치 투자자입니다. 안전마진과 뚝심이 무기입니다."}),
            Map.entry("VRTD", new String[]{"역발상 전략가",
                    "공포 구간에서 과매도된 자산을 여러 개 나눠 담아 단기 반등을 노리는 유형입니다. 분산으로 칼날을 피하며 반등 수익을 수확합니다."}),
            Map.entry("VRTF", new String[]{"역발상 트레이더",
                    "모두가 팔 때 가장 확신 있는 한 곳에 베팅해 단기 반전을 포착하는 대담한 유형입니다. 위기를 기회로 바꾸는 독자적 스타일입니다."}),
            Map.entry("VSLD", new String[]{"배당 정원사",
                    "안정 자산을 넓게 심어 배당이라는 열매를 장기간 수확하는 유형입니다. 분산된 인컴 포트폴리오로 꾸준한 현금흐름을 가꿉니다."}),
            Map.entry("VSLF", new String[]{"배당 수호자",
                    "신뢰하는 소수 배당 우량주를 오래 지키며 안정적인 현금흐름을 최우선으로 삼는 유형입니다. 자산 보존과 배당 성장의 파수꾼입니다."}),
            Map.entry("VSTD", new String[]{"안전 설계자",
                    "원금 보전을 최우선으로 단기 안전자산을 여러 바구니에 나눠 담는 유형입니다. 변동성을 피하면서 작지만 확실한 수익을 쌓습니다."}),
            Map.entry("VSTF", new String[]{"안전 수익가",
                    "안정성과 자산 보존을 중시하며 익숙하고 확실한 소수 자산으로 단기 저위험 수익을 추구합니다. 투자를 저축의 연장선으로 대하는 유형입니다."}));

    private StockMbtiContentBuilder() {}

    static String build(Map<String, Double> scoreMap) {
        double ei = scoreMap.getOrDefault("EI", 0.0);
        double sn = scoreMap.getOrDefault("SN", 0.0);
        double tf = scoreMap.getOrDefault("TF", 0.0);
        double jp = scoreMap.getOrDefault("JP", 0.0);
        boolean hasPersonality = scoreMap.keySet().containsAll(Set.of("EI", "SN", "TF", "JP"));

        String pCode = hasPersonality
                ? letter(ei, "E", "I") + letter(sn, "S", "N") + letter(tf, "T", "F") + letter(jp, "J", "P")
                : "NONE";
        String pAlias = hasPersonality ? PERSONALITY_ALIAS.getOrDefault(pCode, "-") : "-";

        double profit = scoreMap.getOrDefault("수익추구", 50.0);
        double risk   = scoreMap.getOrDefault("리스크허용", 50.0);
        double longT  = scoreMap.getOrDefault("장기투자", 50.0);
        double div    = scoreMap.getOrDefault("분산투자", 50.0);
        String iCode = letter(profit, "G", "V") + letter(risk, "R", "S")
                     + letter(longT, "L", "T") + letter(div, "D", "F");
        String[] meta = INVEST_META.get(iCode);

        String pScores = hasPersonality
                ? String.format("%.0f:%.0f:%.0f:%.0f", ei, sn, tf, jp) : "0:0:0:0";
        String iScores = String.format("%.0f:%.0f:%.0f:%.0f", profit, risk, longT, div);

        return String.join("\n", "V2", pCode, pAlias, iCode, meta[0], meta[1], pScores, iScores);
    }

    private static String letter(double score, String high, String low) {
        return score >= AXIS_CUT ? high : low;
    }
}
