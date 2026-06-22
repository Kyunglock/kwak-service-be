package com.investment.survey.application.service.survey;

import com.investment.survey.application.dto.survey.SurveyAddRequest;
import com.investment.survey.application.dto.survey.SurveyDetailResponse;
import com.investment.survey.application.dto.survey.SystemSurveyCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSurveyServiceImpl implements SystemSurveyService {

    private final SurveyService surveyService;

    @Override
    @Transactional
    public SurveyDetailResponse createSurveyWithQuestions(SystemSurveyCreateRequest request) {
        var created = surveyService.addSurvey(new SurveyAddRequest(
                request.surveyName(),
                request.description(),
                request.surveyTypeCode()
        ));

        Long surveyId = created.surveyId();

        if (request.questions() != null) {
            for (var question : request.questions()) {
                surveyService.addQuestion(surveyId, question);
            }
        }

        log.info("[System] 뉴스 설문 생성 완료 - surveyId: {}, 문항 수: {}",
                surveyId, request.questions() == null ? 0 : request.questions().size());

        return surveyService.getSurveyDetail(surveyId);
    }
}
