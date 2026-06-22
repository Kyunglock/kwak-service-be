package com.investment.stockadvisor.domain.entity.divergence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DivergenceDetectionResult {

    private Long id;
    private String stockCd;
    private Integer fiscalYear;
    private Integer fiscalQuarter;
    private String divergenceType;
    private BigDecimal severity;
    /** JSON 직렬화된 탐지 근거 */
    private String evidence;
    private LocalDateTime detectedAt;
    private LocalDate batchRunDt;
}
