package com.investment.portal.application.service.trade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 없이 규칙만으로 매매 문장을 읽는 빠른 경로.
 *
 * <p>"삼성전자 5주 71,000원에 매도했어" 같은 문장은 사실상 닫힌 문법이다 — 종목 하나,
 * 수량 하나(N주), 단가 하나, 매수/매도 하나. 이걸 매번 31B 모델에 보내면 수 초가 걸리고
 * LLM 서버가 꺼져 있으면 기능 전체가 멈춘다. 프론트가 질문/매매기록 분류를 정규식으로
 * 하는 것과 같은 이유로, 여기서도 확실한 문장만 즉시 처리하고 나머지는 LLM에 넘긴다.
 *
 * <p>원칙은 <b>확신이 없으면 비운다</b>. 숫자가 하나라도 남거나, 매수와 매도가 함께 있거나,
 * 종목이 DB에서 정확히 하나로 확정되지 않으면 {@link Optional#empty()} 를 돌려주고
 * 호출자가 LLM 경로로 폴백한다. 틀린 초안을 빠르게 주는 것보다 느리게 맞는 편이 낫다.
 *
 * <p>종목 확정은 LLM 경로와 똑같이 {@link StockResolver}(DB)가 한다. 여기서 돌려주는
 * ticker 는 이미 DB 로 검증된 정식 티커다.
 */
@Slf4j
@Component
public class RuleBasedTradeExtractor {

    /** 이보다 길면 "문장"이 아니라 "설명"이다 — LLM 이 낫다. */
    private static final int MAX_LENGTH = 120;

    /** 종목명으로 남을 수 있는 토큰 수 상한. 그 이상 남으면 규칙이 문장을 이해하지 못한 것이다. */
    private static final int MAX_NAME_TOKENS = 3;

    private static final Pattern SELL = Pattern.compile(
            "(매도|매각|처분|팔)[가-힣]*|(?<![A-Za-z])(sell|sold)(?![A-Za-z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUY = Pattern.compile(
            "(매수|매입|구매|샀|사서|사고)[가-힣]*|(?<![A-Za-z])(buy|bought)(?![A-Za-z])", Pattern.CASE_INSENSITIVE);

    /** "1주당 / 주당 / 한 주당" — 수량이 아니라 단가 수식어라 수량 탐지 전에 걷어낸다. */
    private static final Pattern PER_SHARE = Pattern.compile("(1\\s*|한\\s*)?주당");

    /** 수량: "5주", "10 주를", "3 shares". 뒤에 식/당 이 오면("주식") 수량이 아니다. */
    private static final Pattern QTY = Pattern.compile(
            "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(?:주(?![식당])|shares?(?![A-Za-z]))", Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_ISO = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})일?");
    private static final Pattern DATE_KO_FULL = Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Pattern DATE_KO_MD = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Pattern DATE_WORD = Pattern.compile("(오늘|어제|그제|그저께)(?:은|는|에|도)?");

    /** 숫자 + 통화 표기. 앞뒤 어느 쪽에 붙어도 읽는다: ₩71,000 / 71,000원 / $230 / 230달러 / 180.5 */
    private static final Pattern MONEY = Pattern.compile(
            "(₩|\\$|KRW|USD)?\\s*(\\d[\\d,]*(?:\\.\\d+)?)\\s*(원|KRW|₩|달러|불|USD|dollars?)?",
            Pattern.CASE_INSENSITIVE);

    /** "7만원", "1억" — 한글 단위 금액은 자릿수 오독 위험이 커서 LLM 에 맡긴다. */
    private static final Pattern KO_NUMBER_UNIT = Pattern.compile("\\d\\s*(만|천|억|백)");

    /** 총액/평균 표기는 단가인지 총액인지 문장만으로 못 가른다. */
    private static final Pattern AMBIGUOUS = Pattern.compile("총|합계|평단|평균|나눠|분할");

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,.;:!?~()\\[\\]\"'·]+");

    /** 종목명이 될 수 없는 조사·수식어·동사 어미. 토큰 전체가 일치할 때만 버린다. */
    private static final Set<String> STOPWORDS = Set.of(
            "단가", "단가는", "단가로", "단가가", "가격", "가격은", "가격으로", "가격에", "가격이",
            "에", "에서", "으로", "로", "을", "를", "은", "는", "이", "가", "도", "의", "만",
            "씩", "정도", "쯤", "각", "개", "건", "것", "좀", "조금", "약", "대략", "그냥", "방금", "막",
            "이번에", "이번엔", "추가로", "추가", "더", "다", "다시", "또", "전부", "모두",
            "했어", "했다", "했음", "했습니다", "했어요", "했지", "했고", "했는데", "했네", "했다고",
            "해서", "하고", "함", "합니다", "했", "해", "완료", "체결", "체결됨", "체결됐어",
            "어", "야", "요", "음", "ㅇㅇ", "ㅋㅋ");

    /** 종목명 뒤에 붙는 조사. 원형으로 못 찾을 때만 떼어 본다 ("에코프로"의 '로'를 함부로 떼지 않기 위해). */
    private static final Pattern TRAILING_PARTICLE =
            Pattern.compile("(으로|에서|부터|까지|이랑|을|를|은|는|이|가|도|만|의|에|로|랑|과|와)$");

    private final StockResolver stockResolver;
    private final boolean enabled;

    public RuleBasedTradeExtractor(
            StockResolver stockResolver,
            @Value("${trade.extraction.rules.enabled:true}") boolean enabled) {
        this.stockResolver = stockResolver;
        this.enabled = enabled;
        log.info("[TradeCapture] 규칙 기반 빠른 경로 사용: {}", enabled);
    }

    /**
     * @param text  사용자 문장
     * @param today 상대 날짜("어제")와 연도 없는 날짜("10월 30일")의 기준일
     * @return 확신할 수 있을 때만 한 건. 그 외는 empty — 호출자가 LLM 으로 폴백한다.
     */
    public Optional<ExtractedTrade> extract(String text, LocalDate today) {
        if (!enabled || text == null) {
            return Optional.empty();
        }
        String rest = text.strip().replaceAll("\\s+", " ");
        if (rest.isEmpty() || rest.length() > MAX_LENGTH || text.contains("\n")) {
            return Optional.empty();
        }
        if (AMBIGUOUS.matcher(rest).find() || KO_NUMBER_UNIT.matcher(rest).find()) {
            return Optional.empty();
        }

        // 1) 매수/매도 — 정확히 한 쪽만 있어야 한다
        int sells = count(SELL, rest);
        int buys = count(BUY, rest);
        if (sells + buys == 0 || (sells > 0 && buys > 0)) {
            return Optional.empty();
        }
        String type = sells > 0 ? "SELL" : "BUY";
        rest = SELL.matcher(rest).replaceAll(" ");
        rest = BUY.matcher(rest).replaceAll(" ");

        // 2) 수량 — 정확히 하나
        rest = PER_SHARE.matcher(rest).replaceAll(" ");
        Matcher qtyMatcher = QTY.matcher(rest);
        if (!qtyMatcher.find()) {
            return Optional.empty();
        }
        BigDecimal qty = number(qtyMatcher.group(1));
        if (qtyMatcher.find() || qty == null || qty.signum() <= 0) {
            return Optional.empty();
        }
        rest = QTY.matcher(rest).replaceFirst(" ");

        // 3) 날짜 — 없거나 하나
        DateResult dateResult = extractDate(rest, today);
        if (dateResult == null) {
            return Optional.empty();
        }
        rest = dateResult.rest();

        // 4) 단가 — 남은 숫자가 정확히 하나여야 그것이 단가다
        Matcher moneyMatcher = MONEY.matcher(rest);
        if (!moneyMatcher.find()) {
            return Optional.empty();
        }
        BigDecimal price = number(moneyMatcher.group(2));
        String currency = currency(moneyMatcher.group(1), moneyMatcher.group(3));
        if (moneyMatcher.find() || price == null || price.signum() <= 0) {
            return Optional.empty();
        }
        rest = MONEY.matcher(rest).replaceFirst(" ");

        // 5) 남은 토큰이 종목명이어야 한다
        List<String> tokens = new ArrayList<>();
        for (String token : TOKEN_SPLIT.split(rest)) {
            if (!token.isBlank() && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty() || tokens.size() > MAX_NAME_TOKENS) {
            return Optional.empty();
        }

        Resolved resolved = resolveName(tokens);
        if (resolved == null) {
            return Optional.empty();
        }

        log.debug("[TradeCapture] 규칙 추출 - {} {} {}주 @{} {}", resolved.rawName(), type, qty, price, currency);
        return Optional.of(new ExtractedTrade(
                resolved.rawName(), resolved.stockCd(), type, qty, price, null, dateResult.date(), currency));
    }

    // ── 종목 ─────────────────────────────────────────────────────────────────────

    private record Resolved(String rawName, String stockCd) {}

    /**
     * 토큰 조합을 원형 → 조사 제거 순으로 DB 에 물어본다.
     * 서로 다른 종목으로 확정되는 후보가 둘 이상이면 문장이 애매한 것이므로 포기한다.
     */
    private Resolved resolveName(List<String> tokens) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(String.join(" ", tokens));
        candidates.add(String.join("", tokens));
        if (tokens.size() > 1) {
            candidates.add(tokens.get(0));
        }

        Resolved first = null;
        for (String candidate : candidates) {
            for (String variant : variants(candidate)) {
                StockResolver.Resolution resolution = stockResolver.resolve(variant, null);
                if (!resolution.isResolved()) {
                    continue;
                }
                if (first == null) {
                    first = new Resolved(variant, resolution.stockCd());
                } else if (!first.stockCd().equals(resolution.stockCd())) {
                    return null;
                }
                break; // 같은 후보의 다른 변형은 더 보지 않는다
            }
        }
        return first;
    }

    private List<String> variants(String candidate) {
        List<String> out = new ArrayList<>(2);
        out.add(candidate);
        Matcher m = TRAILING_PARTICLE.matcher(candidate);
        if (m.find() && m.start() > 0) {
            out.add(candidate.substring(0, m.start()));
        }
        return out;
    }

    // ── 날짜 ─────────────────────────────────────────────────────────────────────

    private record DateResult(LocalDate date, String rest) {}

    /** 날짜 표기를 찾아 제거한다. 둘 이상 있거나 해석 불가면 null(=포기). */
    private DateResult extractDate(String rest, LocalDate today) {
        LocalDate found = null;
        int hits = 0;

        Matcher m = DATE_ISO.matcher(rest);
        if (m.find()) {
            hits++;
            found = safeDate(m.group(1), m.group(2), m.group(3));
            rest = DATE_ISO.matcher(rest).replaceFirst(" ");
        }
        m = DATE_KO_FULL.matcher(rest);
        if (m.find()) {
            hits++;
            found = safeDate(m.group(1), m.group(2), m.group(3));
            rest = DATE_KO_FULL.matcher(rest).replaceFirst(" ");
        }
        m = DATE_KO_MD.matcher(rest);
        if (m.find()) {
            hits++;
            LocalDate d = safeDate(String.valueOf(today.getYear()), m.group(1), m.group(2));
            // 연도 없이 "12월 20일"이라 하면 아직 안 온 날짜는 작년 얘기다
            found = (d != null && d.isAfter(today)) ? d.minusYears(1) : d;
            rest = DATE_KO_MD.matcher(rest).replaceFirst(" ");
        }
        m = DATE_WORD.matcher(rest);
        if (m.find()) {
            hits++;
            found = switch (m.group(1)) {
                case "어제" -> today.minusDays(1);
                case "그제", "그저께" -> today.minusDays(2);
                default -> today;
            };
            rest = DATE_WORD.matcher(rest).replaceFirst(" ");
        }

        if (hits > 1 || (hits == 1 && found == null)) {
            return null;
        }
        return new DateResult(found, rest);
    }

    private LocalDate safeDate(String y, String mo, String d) {
        try {
            return LocalDate.of(Integer.parseInt(y), Integer.parseInt(mo), Integer.parseInt(d));
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────────

    private static int count(Pattern p, String s) {
        Matcher m = p.matcher(s);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static BigDecimal number(String raw) {
        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String currency(String prefix, String suffix) {
        String mark = (prefix != null ? prefix : suffix);
        if (mark == null) {
            return null;
        }
        String upper = mark.toUpperCase();
        if (upper.contains("KRW") || mark.contains("원") || mark.contains("₩")) {
            return "KRW";
        }
        if (upper.contains("USD") || mark.contains("$") || mark.contains("달러") || mark.contains("불")
                || upper.startsWith("DOLLAR")) {
            return "USD";
        }
        return null;
    }
}
