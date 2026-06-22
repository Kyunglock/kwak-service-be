package com.investment.stockadvisor.scheduler;

import com.investment.stockadvisor.application.service.divergence.DerivedMetricsService;
import com.investment.stockadvisor.application.service.divergence.DivergenceDetectionService;
import com.investment.stockadvisor.application.service.divergence.DivergenceInterpretationService;
import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;
import com.investment.stockadvisor.domain.repository.divergence.DivergenceDetectionResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DivergenceDetectionScheduler {

    private final DerivedMetricsService derivedMetricsService;
    private final DivergenceDetectionService detectionService;
    private final DivergenceInterpretationService interpretationService;
    private final DivergenceDetectionResultMapper resultMapper;

    /**
     * 미국장 마감(EST 16:00) 후 한국 시간 매일 새벽 6시에 실행
     * Step 1: 파생 지표 갱신
     * Step 2: Divergence 탐지 저장
     * Step 3: LLM 해석 생성 및 Redis 캐싱
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void runDivergenceDetectionPipeline() {
        log.info("[DivergenceBatch] pipeline started");
        try {
            derivedMetricsService.computeAndSaveAll();
            log.info("[DivergenceBatch] step1 derived metrics done");

            detectionService.detectAndSaveAll();
            log.info("[DivergenceBatch] step2 detection done");

            interpretBatch();
            log.info("[DivergenceBatch] step3 interpretation done");
        } catch (Exception e) {
            log.error("[DivergenceBatch] pipeline failed", e);
        }
    }

    private void interpretBatch() {
        List<DivergenceDetectionResult> todayResults = resultMapper.findByBatchRunDt(LocalDate.now());
        int success = 0, fail = 0;
        for (DivergenceDetectionResult result : todayResults) {
            try {
                interpretationService.interpret(result);
                success++;
            } catch (Exception e) {
                log.warn("[DivergenceBatch] interpretation skipped stockCd={} type={}: {}",
                        result.getStockCd(), result.getDivergenceType(), e.getMessage());
                fail++;
            }
        }
        log.info("[DivergenceBatch] interpretation complete success={} fail={}", success, fail);
    }
}
