package com.investment.stockadvisor.application.service.divergence;

import com.investment.stockadvisor.application.dto.divergence.DivergenceResultResponse;
import com.investment.stockadvisor.domain.entity.divergence.DivergenceDetectionResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DivergenceDetectionConverter {

    DivergenceResultResponse toResponse(DivergenceDetectionResult entity);

    List<DivergenceResultResponse> toResponseList(List<DivergenceDetectionResult> entities);
}
