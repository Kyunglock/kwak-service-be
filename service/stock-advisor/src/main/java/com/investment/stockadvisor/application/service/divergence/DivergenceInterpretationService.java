package com.investment.stockadvisor.application.service.divergence;

import com.investment.stockadvisor.application.dto.divergence.DivergenceInterpretationResponse;
import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;

import java.util.List;

public interface DivergenceInterpretationService {
    DivergenceInterpretationResponse interpret(DivergenceDetectionResult result);
    List<DivergenceInterpretationResponse> interpretByStockCd(String stockCd);
}
