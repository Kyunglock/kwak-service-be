package com.investment.survey.application.service.survey;

import java.util.List;

import org.springframework.stereotype.Service;

import com.investment.survey.application.dto.survey.SurveyStatsResponse;
import com.investment.survey.domain.entity.SurveyStats;
import com.investment.survey.domain.repository.SurveyStatsMapper;
import kwak.common.application.dto.PageResponse;

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

    @Override
    public PageResponse<SurveyStatsResponse> getSurveyStatsResponsesPaged(
            String userId, String keyword, int page, int size, String sort) {
        String[] orderParts = parseSortForStats(sort);
        int offset = page * size;
        int total = statsMapper.countSurveyStats(userId, keyword);
        List<SurveyStatsResponse> content = statsMapper
                .findAllSurveyStatsPaged(userId, keyword, offset, size, orderParts[0], orderParts[1])
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, page + 1, size, total);
    }

    private String[] parseSortForStats(String sort) {
        if (sort == null || sort.isBlank()) return new String[]{"A.REG_DT", "DESC"};
        String[] parts = sort.split(",");
        String col = switch (parts[0].trim()) {
            case "surveyName" -> "A.SURVEY_NAME";
            default -> "A.REG_DT";
        };
        String dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? "ASC" : "DESC";
        return new String[]{col, dir};
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
