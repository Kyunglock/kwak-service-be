package com.investment.stockadvisor.domain.detector;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class DetectionResult {

    private String type;
    /** 0.0 ~ 1.0 */
    private BigDecimal severity;
    private Map<String, Object> evidence;
    private Integer fiscalYear;
    private Integer fiscalQuarter;
    /** 섹터 퍼센타일 severity 계산에 사용할 메트릭 이름 */
    private String severityMetric;
    /** 섹터 퍼센타일 계산에 사용할 원시 값 */
    private BigDecimal rawMetricValue;
}
