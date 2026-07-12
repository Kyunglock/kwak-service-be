package com.investment.stockadvisor.application.service.guru;

import com.investment.stockadvisor.application.dto.guru.GuruRecentActivityResponse;
import com.investment.stockadvisor.application.dto.guru.GuruRecentActivitySearchRequest;
import com.investment.stockadvisor.domain.entity.guru.GuruRecentActivity;
import com.investment.stockadvisor.domain.repository.guru.GuruRecentActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuruRecentActivityServiceImpl implements GuruRecentActivityService {

    private final GuruRecentActivityMapper guruRecentActivityMapper;
    private final GuruConverter guruConverter;

    @Override
    public List<GuruRecentActivityResponse> findGuruRecentActivityAll(GuruRecentActivitySearchRequest searchRequest) {
        List<GuruRecentActivity> list = guruRecentActivityMapper.findGuruRecentActivityAll(searchRequest);
        return guruConverter.toActivityResponseList(list);
    }

    @Override
    public GuruRecentActivityResponse findGuruRecentActivityById(Long id) {
        GuruRecentActivity entity = guruRecentActivityMapper.findById(id);
        return guruConverter.toActivityResponse(entity);
    }
}
