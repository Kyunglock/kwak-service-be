package com.investment.survey.application.service.survey;

import java.util.List;

import org.springframework.stereotype.Service;

import com.investment.survey.application.dto.survey.SurveyStatsResponse;
import com.investment.survey.domain.entity.SurveyStats;
import com.investment.survey.domain.repository.SurveyStatsMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyStatsServiceImpl implements SurveyStatsService {

    private final SurveyStatsMapper statsMapper;

    @Override
    public List<SurveyStatsResponse> getSurveyStatsResponses(String userId) {
        return statsMapper.findAllSurveyStats(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SurveyStatsResponse toResponse(SurveyStats s) {
        return new SurveyStatsResponse(
                s.getSurveyId(),
                s.getResponseId(),
                s.getSurveyName(),
                s.getRegDt(),
                s.getTotalParticipants(),
                s.getStatusCode()
        );
    }
}
