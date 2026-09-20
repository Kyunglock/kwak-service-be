package com.investment.portal.application.service.trade;

import com.investment.portal.domain.repository.stock.StockRef;
import com.investment.portal.domain.repository.stock.StockResolveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LLM이 읽어낸 종목 표기를 DB의 정식 티커로 확정한다.
 *
 * <p>LLM이 준 ticker 값도 그대로 믿지 않고 반드시 DB에 물어본다 — 프롬프트로 "추측 금지"를
 * 지시해도 모델은 종종 그럴듯한 티커를 만들어낸다. 여기서 막지 못하면 존재하지 않는 종목이
 * 사용자 포트폴리오에 저장된다.
 */
@Component
@RequiredArgsConstructor
public class StockResolver {

    private static final int MAX_CANDIDATES = 5;

    private final StockResolveMapper stockResolveMapper;

    /**
     * @param stockCd    확정된 정식 티커. 확정 실패 시 null
     * @param stockNm    표시용 종목명
     * @param candidates 확정 실패 시 사용자에게 제시할 후보 (없으면 빈 목록)
     */
    public record Resolution(String stockCd, String stockNm, List<StockRef> candidates) {

        static Resolution resolved(String stockCd, String stockNm) {
            return new Resolution(stockCd, stockNm, List.of());
        }

        static Resolution unresolved(List<StockRef> candidates) {
            return new Resolution(null, null, candidates == null ? List.of() : candidates);
        }

        public boolean isResolved() {
            return stockCd != null;
        }
    }

    public Resolution resolve(String rawName, String rawTicker) {
        // 1) LLM이 티커를 읽었다면 그것부터 검증
        Resolution byTicker = byTicker(rawTicker);
        if (byTicker != null) {
            return byTicker;
        }

        if (rawName == null || rawName.isBlank()) {
            return Resolution.unresolved(List.of());
        }
        String name = rawName.trim();
        String upper = name.toUpperCase();

        // 2) 종목명 자리에 티커가 들어온 경우 ("AAPL 10주 샀어")
        Resolution nameAsTicker = byTicker(upper);
        if (nameAsTicker != null) {
            return nameAsTicker;
        }

        // 3) 종목명 정확 일치. 진짜 동명이의일 때만 사용자가 고르게 한다
        List<StockRef> exact = distinctByTicker(stockResolveMapper.findByExactName(upper));
        if (exact.size() == 1) {
            StockRef ref = exact.get(0);
            return Resolution.resolved(ref.stockCd(), ref.stockNm());
        }
        if (exact.size() > 1) {
            return Resolution.unresolved(exact.subList(0, Math.min(exact.size(), MAX_CANDIDATES)));
        }

        // 4) 부분 일치는 후보 제시까지만 — 자동 선택하지 않는다.
        //    "삼성"으로 삼성전자를 자동 선택하면 삼성SDI를 산 사람의 기록이 조용히 틀어진다.
        return Resolution.unresolved(
                distinctByTicker(stockResolveMapper.findByPartialName(upper, MAX_CANDIDATES)));
    }

    /**
     * 같은 티커를 가리키는 행을 하나로 합친다.
     *
     * <p>조회는 tbl_companies(영문·한글명)와 tbl_stock_info(종목명)를 UNION 하므로,
     * "애플"처럼 양쪽에 다 있는 종목은 이름만 다른 같은 티커가 여러 줄로 돌아온다.
     * 합치지 않으면 후보가 하나뿐인데도 동명이의로 보고 사용자에게 고르라고 묻게 된다.
     *
     * <p>이름은 먼저 나온 것을 쓴다 — 조회 순서상 tbl_companies 쪽이 앞서고,
     * 그쪽이 표시용으로 더 정확하다.
     */
    private List<StockRef> distinctByTicker(List<StockRef> refs) {
        Map<String, StockRef> byTicker = new LinkedHashMap<>();
        for (StockRef ref : refs) {
            if (ref != null && ref.stockCd() != null) {
                byTicker.putIfAbsent(ref.stockCd(), ref);
            }
        }
        return List.copyOf(byTicker.values());
    }

    /** 티커 후보를 DB에서 검증. 확정되면 Resolution, 아니면 null. */
    private Resolution byTicker(String rawTicker) {
        if (rawTicker == null || rawTicker.isBlank()) {
            return null;
        }
        String ticker = rawTicker.trim().toUpperCase();
        Optional<String> canonical = stockResolveMapper.findCanonicalTicker(ticker);
        if (canonical.isEmpty()) {
            return null;
        }
        String stockCd = canonical.get();
        return Resolution.resolved(
                stockCd,
                stockResolveMapper.findStockNameByTicker(stockCd).orElse(stockCd));
    }
}
