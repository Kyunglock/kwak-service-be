package com.investment.portal.api.controller.stock;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import com.investment.portal.application.dto.stock.StockWithLatestPriceResponse;
import com.investment.portal.application.service.stock.StockPriceQueryService;
import com.investment.portal.infrastructure.cache.StockPriceCacheStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kwak.common.application.dto.RokResponse;
import kwak.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Tag(name = "주가 조회", description = "종목 현재가/종가 조회 API")
@RestController
@RequestMapping("/api/v1/stocks/price")
@RequiredArgsConstructor
public class StockPriceController {

    private final StockPriceCacheStore cacheStore;
    private final StockPriceQueryService queryService;

    /**
     * 특정 종목 현재가 조회
     * 1순위: 캐시(실시간) → 2순위: DB(종가) fallback
     */
    @Operation(summary = "종목 현재가 조회", description = "캐시에서 실시간 가격을 조회하고, 없으면 DB 종가로 fallback합니다")
    @GetMapping("/{stockCd}")
    public RokResponse<StockPriceSnapshot> getCurrentPrice(
            @Parameter(description = "종목코드", example = "AAPL")
            @PathVariable String stockCd) {

        StockPriceSnapshot snapshot = queryService.getPrice(stockCd);
        if (snapshot == null) {
            return RokResponse.<StockPriceSnapshot>builder()
                    .success(false)
                    .message("해당 종목의 가격 정보가 없습니다: " + stockCd)
                    .build();
        }

        return RokResponse.<StockPriceSnapshot>builder()
                .success(true)
                .message("조회 성공")
                .data(snapshot)
                .build();
    }

    /**
     * 캐시에 있는 전체 종목 현재가 조회
     */
    @Operation(summary = "전체 종목 현재가 조회", description = "캐시에 보관 중인 모든 종목의 실시간 가격을 조회합니다")
    @GetMapping
    public RokResponse<Collection<StockPriceSnapshot>> getAllPrices() {
        Collection<StockPriceSnapshot> snapshots = cacheStore.getAll();

        return RokResponse.<Collection<StockPriceSnapshot>>builder()
                .success(true)
                .message("조회 성공 (" + snapshots.size() + "건)")
                .data(snapshots)
                .build();
    }

    /**
     * 전체 기업 + 최근 종가 조인 조회
     */
    @Operation(summary = "기업 정보 + 최근 종가 조회", description = "TBL_COMPANIES와 최근 종가를 조인하여 기업별 최신 가격 정보를 조회합니다")
    @GetMapping("/with-company")
    public ResponseEntity<?> findAllWithLatestPrice() {
        return ResponseUtil.success(queryService.getAllWithLatestPrice());
    }
}
