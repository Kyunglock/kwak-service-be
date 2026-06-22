package com.investment.portal.infrastructure.sse;

import com.investment.portal.application.dto.stock.StockPriceSnapshot;
import com.investment.portal.application.service.stock.StockPriceQueryService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE(Server-Sent Events) 기반 실시간 주가 푸시 서비스
 *
 * 클라이언트가 SSE 구독 → 서버가 캐시 갱신 시 자동 push
 * 클라이언트는 매초 SELECT API를 호출할 필요 없음
 *
 * - SseEmitter timeout을 무제한(-1)으로 설정하여 AsyncRequestTimeoutException 방지
 * - 30초 간격 heartbeat로 연결 유지 및 죽은 커넥션 탐지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceSseEmitterService {

    private static final long SSE_TIMEOUT = -1L; // 무제한 (서버에서 직접 관리)

    private final StockPriceQueryService queryService;

    /**
     * 종목코드별 SSE 구독자 목록
     * key: stockCd, value: 해당 종목을 구독 중인 SseEmitter 리스트
     */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByStock = new ConcurrentHashMap<>();

    /**
     * 전체 종목 구독자 (모든 가격 변동을 받는 클라이언트)
     */
    private final CopyOnWriteArrayList<SseEmitter> globalEmitters = new CopyOnWriteArrayList<>();

    /**
     * 특정 종목 SSE 구독
     * 구독 시 캐시 또는 DB 종가에서 초기 가격 데이터를 즉시 전송
     */
    public SseEmitter subscribe(String stockCd) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        CopyOnWriteArrayList<SseEmitter> emitters = emittersByStock.computeIfAbsent(
                stockCd, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(stockCd, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(stockCd, emitter);
        });
        emitter.onError(e -> removeEmitter(stockCd, emitter));

        // 초기 데이터 전송: 캐시 → DB 종가 fallback
        sendInitialData(emitter, stockCd);

        log.debug("[SSE] 종목 {} 구독 추가 (현재 {}명)", stockCd, emitters.size());
        return emitter;
    }

    /**
     * 전체 종목 SSE 구독
     * 구독 시 캐시에 있는 전체 가격 데이터를 즉시 전송, 캐시가 비어있으면 DB 종가 전송
     */
    public SseEmitter subscribeAll() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        globalEmitters.add(emitter);

        emitter.onCompletion(() -> globalEmitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            globalEmitters.remove(emitter);
        });
        emitter.onError(e -> globalEmitters.remove(emitter));

        // 초기 데이터 전송: 캐시에 있는 전체 가격
        sendInitialDataAll(emitter);

        log.debug("[SSE] 전체 종목 구독 추가 (현재 {}명)", globalEmitters.size());
        return emitter;
    }

    /**
     * 특정 종목 가격 변동 시 해당 구독자 + 전체 구독자에게 push
     */
    public void broadcast(StockPriceSnapshot snapshot) {
        String stockCd = snapshot.getStockCd();

        // 종목별 구독자에게 push
        CopyOnWriteArrayList<SseEmitter> stockEmitters = emittersByStock.get(stockCd);
        if (stockEmitters != null) {
            sendToEmitters(stockEmitters, stockCd, snapshot);
        }

        // 전체 구독자에게 push
        sendToEmitters(globalEmitters, "price-update", snapshot);
    }

    /**
     * 여러 종목 일괄 broadcast
     */
    public void broadcastAll(List<StockPriceSnapshot> snapshots) {
        for (StockPriceSnapshot snapshot : snapshots) {
            broadcast(snapshot);
        }
    }

    /**
     * 30초 간격 heartbeat 전송
     * - SSE 연결 유지 (프록시/로드밸런서 idle timeout 방지)
     * - 죽은 커넥션 탐지 후 자동 정리
     */
    @Scheduled(fixedRate = 30_000)
    public void sendHeartbeat() {
        // 종목별 구독자 heartbeat
        emittersByStock.forEach((stockCd, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            }
        });

        // 전체 구독자 heartbeat
        for (SseEmitter emitter : globalEmitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                globalEmitters.remove(emitter);
            }
        }
    }

    /**
     * 개별 종목 구독 시 초기 데이터 전송 (캐시 → DB 종가 fallback)
     */
    private void sendInitialData(SseEmitter emitter, String stockCd) {
        try {
            StockPriceSnapshot snapshot = queryService.getPrice(stockCd);
            if (snapshot != null) {
                emitter.send(SseEmitter.event()
                        .name(stockCd)
                        .data(snapshot));
                log.debug("[SSE] 초기 데이터 전송 완료: {}", stockCd);
            }
        } catch (IOException e) {
            log.debug("[SSE] 초기 데이터 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 전체 구독 시 초기 데이터 전송 (캐시에 있는 전체 가격)
     */
    private void sendInitialDataAll(SseEmitter emitter) {
        try {
            Collection<StockPriceSnapshot> snapshots = queryService.getAllPrices();
            if (!snapshots.isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name("price-update")
                        .data(snapshots));
                log.debug("[SSE] 전체 초기 데이터 전송 완료: {}건", snapshots.size());
            }
        } catch (IOException e) {
            log.debug("[SSE] 전체 초기 데이터 전송 실패: {}", e.getMessage());
        }
    }

    private void sendToEmitters(CopyOnWriteArrayList<SseEmitter> emitters, String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("[SSE] 전송 실패로 emitter 제거: {}", e.getMessage());
            }
        }
    }

    private void removeEmitter(String stockCd, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByStock.get(stockCd);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByStock.remove(stockCd);
            }
        }
    }

    /**
     * 현재 구독자 수 조회 (모니터링용)
     */
    public int getSubscriberCount() {
        int stockSubscribers = emittersByStock.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
        return stockSubscribers + globalEmitters.size();
    }

    @PreDestroy
    public void destroy() {
        emittersByStock.values().forEach(emitters -> {
            for (SseEmitter emitter : emitters) {
                emitter.complete();
            }
        });
        for (SseEmitter emitter : globalEmitters) {
            emitter.complete();
        }
        log.info("[SSE] 모든 SSE 연결 종료");
    }
}
