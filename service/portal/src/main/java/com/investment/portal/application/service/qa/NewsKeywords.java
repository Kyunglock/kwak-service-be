package com.investment.portal.application.service.qa;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 뉴스 검색용 종목 키워드. 종목 해석 결과의 이름은 법인명("Apple Inc.")이라
 * 기사 제목("Apple shares fall", "애플 급락")과 그대로는 거의 매칭되지 않는다.
 */
final class NewsKeywords {

    private static final Pattern CORP_SUFFIX = Pattern.compile(
            "(?i)[\\s,]+(class\\s+[a-c]|inc|incorporated|corp|corporation|co|company|ltd|limited"
                    + "|plc|holdings?|group|n\\.?v|s\\.?a|ag|se)\\.?$");
    private static final Pattern LEADING_THE = Pattern.compile("(?i)^the\\s+");
    private static final Pattern KR_EXCHANGE_SUFFIX = Pattern.compile("\\.(KS|KQ|KX)$");

    /** "F", "GE" 같은 짧은 영문 티커는 LIKE '%F%' 가 되어 아무 기사나 걸린다. */
    private static final int MIN_ASCII_KEYWORD_LENGTH = 3;

    private NewsKeywords() {
    }

    /** "Apple Inc." → "Apple", "The Goldman Sachs Group, Inc." → "Goldman Sachs". */
    static String shortName(String name) {
        if (name == null) {
            return null;
        }
        String result = name.trim();
        String prev;
        do {
            prev = result;
            result = CORP_SUFFIX.matcher(result).replaceFirst("").trim();
        } while (!result.equals(prev));
        result = LEADING_THE.matcher(result).replaceFirst("").trim();
        return result.isEmpty() ? name.trim() : result;
    }

    /** 약식 영문명(또는 국내 종목명), 티커(거래소 접미사 제거), 한글명 순. */
    static List<String> build(String stockNm, String stockCd, String koreanName) {
        Set<String> keywords = new LinkedHashSet<>();
        add(keywords, shortName(stockNm));
        add(keywords, stockCd == null ? null : KR_EXCHANGE_SUFFIX.matcher(stockCd).replaceFirst(""));
        add(keywords, koreanName);
        return List.copyOf(keywords);
    }

    private static void add(Set<String> keywords, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String k = keyword.trim();
        boolean ascii = k.chars().allMatch(c -> c < 128);
        if (ascii && k.length() < MIN_ASCII_KEYWORD_LENGTH) {
            return;
        }
        keywords.add(k);
    }
}
