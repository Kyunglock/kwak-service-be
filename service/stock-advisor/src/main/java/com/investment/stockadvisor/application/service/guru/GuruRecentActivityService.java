package com.investment.stockadvisor.application.service.guru;

import com.investment.stockadvisor.application.dto.guru.GuruRecentActivityResponse;
import com.investment.stockadvisor.application.dto.guru.GuruRecentActivitySearchRequest;

import java.util.List;

public interface GuruRecentActivityService {

    List<GuruRecentActivityResponse> findGuruRecentActivityAll(GuruRecentActivitySearchRequest searchRequest);

    GuruRecentActivityResponse findGuruRecentActivityById(Long id);
}
