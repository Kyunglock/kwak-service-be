package com.investment.survey.application.service.survey;

import com.investment.survey.application.dto.response.AnswerRequest;
import com.investment.survey.application.dto.response.SurveySubmitRequest;
import com.investment.survey.application.dto.response.SurveySubmitResponse;
import com.investment.survey.application.dto.response.SurveyWithMyResponse;
import com.investment.survey.application.dto.result.SurveyResultResponse;
import kwak.common.application.event.ActivityEvent;
import com.investment.survey.domain.entity.*;
import kwak.common.application.dto.PageResponse;
import com.investment.survey.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyResponseServiceImpl implements SurveyResponseService {

    private final SurveyMapper surveyMapper;
    private final SurveyOptionMapper optionMapper;
    private final UserSurveyResponseMapper responseMapper;
    private final SurveyAnswerMapper answerMapper;
    private final SurveyResultMapper resultMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SurveySubmitResponse submitSurvey(String userId, SurveySubmitRequest request) {
        // 설문 존재 확인
        Survey survey = surveyMapper.findSurveyById(request.surveyId());
        if (survey == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + request.surveyId());
        }

        // 1. 응답 레코드 생성
        UserSurveyResponse response = UserSurveyResponse.builder()
                .userId(userId)
                .surveyId(request.surveyId())
                .build();
        responseMapper.insertSurveyResponse(response);

        // 2. 선택지 점수 일괄 조회
        List<Long> optionIds = request.answers().stream()
                .map(AnswerRequest::selectedOptionId)
                .toList();

        Map<Long, SurveyOption> optionMap = optionIds.stream()
                .map(optionMapper::findOptionById)
                .filter(o -> o != null)
                .collect(Collectors.toMap(SurveyOption::getOptionId, o -> o));

        // 3. 개별 답변 저장 + 총점 계산
        int totalScore = 0;
        List<SurveyAnswer> answers = new ArrayList<>();

        for (AnswerRequest ansReq : request.answers()) {
            SurveyOption option = optionMap.get(ansReq.selectedOptionId());
            int score = option != null && option.getScore() != null ? option.getScore() : 0;
            String value = option != null ? option.getOptionValue() : null;

            answers.add(SurveyAnswer.builder()
                    .responseId(response.getResponseId())
                    .questionId(ansReq.questionId())
                    .selectedOptionId(ansReq.selectedOptionId())
                    .selectedValue(value)
                    .answerScore(score)
                    .build());

            totalScore += score;
        }

        answerMapper.batchInsertAnswers(answers);

        // 4. 위험 성향 판정
        String riskProfile = determineRiskProfile(totalScore, request.answers().size());

        // 5. 응답 완료 처리
        UserSurveyResponse completed = UserSurveyResponse.builder()
                .responseId(response.getResponseId())
                .statusCode("COMPLETED")
                .completedAt(LocalDateTime.now())
                .totalScore(totalScore)
                .riskProfileCode(riskProfile)
                .build();
        responseMapper.updateSurveyResponse(completed);

        // 6. 결과 분석 저장 (기존 활성 결과 비활성화)
        resultMapper.deactivateSurveyResultsByUserAndSurvey(userId, request.surveyId());

        SurveyResult result = SurveyResult.builder()
                .userId(userId)
                .surveyId(request.surveyId())
                .riskScore(totalScore)
                .riskLevelCode(riskProfile)
                .recommendation(generateRecommendation(riskProfile))
                .portfolioSuggestion(generatePortfolioSuggestion(riskProfile))
                .validUntil(LocalDateTime.now().plusYears(1))
                .build();
        resultMapper.insertSurveyResult(result);

        log.info("[SurveyResponse] 설문 제출 완료 - userId: {}, surveyId: {}, score: {}, profile: {}",
                userId, request.surveyId(), totalScore, riskProfile);

        eventPublisher.publishEvent(ActivityEvent.of(
                userId, "SURVEY_SUBMIT", "SURVEY", String.valueOf(request.surveyId()), "설문 제출"));

        return new SurveySubmitResponse(
                response.getResponseId(),
                request.surveyId(),
                userId,
                "COMPLETED",
                totalScore,
                riskProfile,
                completed.getCompletedAt(),
                completed.getTotalParticipants()
        );
    }

    @Override
    public List<SurveySubmitResponse> getMyResponses(String userId) {
        return responseMapper.findSurveyResponsesByUserId(userId).stream()
                .map(this::toSubmitResponse)
                .toList();
    }

    @Override
    public SurveyResultResponse getMyResult(String userId, Long surveyId) {
        SurveyResult result = resultMapper.findActiveSurveyResultByUserAndSurvey(userId, surveyId);
        if (result == null) {
            return null;
        }
        return toResultResponse(result);
    }

    @Override
    public List<SurveyResultResponse> getMyResults(String userId) {
        return resultMapper.findSurveyResultsByUserId(userId).stream()
                .map(this::toResultResponse)
                .toList();
    }

    @Override
    public List<SurveyWithMyResponse> getSurveyWithMyResponses(String userId) {
        return surveyMapper.findSurveyWithMyResponses(userId).stream()
                .map(this::toSurveyWithMyResponse)
                .toList();
    }

    @Override
    public PageResponse<SurveyWithMyResponse> getSurveyWithMyResponsesPaged(
            String userId, String keyword, int page, int size, String sort) {
        String[] orderParts = parseSortForSurvey(sort);
        int offset = page * size;
        int total = surveyMapper.countSurveyWithMyResponses(userId, keyword);
        List<SurveyWithMyResponse> content = surveyMapper
                .findSurveyWithMyResponsesPaged(userId, keyword, offset, size, orderParts[0], orderParts[1])
                .stream()
                .map(this::toSurveyWithMyResponse)
                .toList();
        return PageResponse.of(content, page + 1, size, total);
    }

    private String[] parseSortForSurvey(String sort) {
        if (sort == null || sort.isBlank()) return new String[]{"S.REG_DT", "DESC"};
        String[] parts = sort.split(",");
        String col = switch (parts[0].trim()) {
            case "surveyName" -> "S.SURVEY_NAME";
            default -> "S.REG_DT";
        };
        String dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? "ASC" : "DESC";
        return new String[]{col, dir};
    }

    /**
     * 총점 기반 위험 성향 판정
     * 평균 점수 기준: 1~2 보수적, 2~3 중립적, 3~5 공격적
     */
    private String determineRiskProfile(int totalScore, int questionCount) {
        if (questionCount == 0) return "MODERATE";

        double avgScore = (double) totalScore / questionCount;

        if (avgScore <= 2.0) {
            return "CONSERVATIVE";
        } else if (avgScore <= 3.5) {
            return "MODERATE";
        } else {
            return "AGGRESSIVE";
        }
    }

    private String generateRecommendation(String riskProfile) {
        return switch (riskProfile) {
            case "CONSERVATIVE" -> "안정적인 자산 위주의 포트폴리오를 추천합니다. 채권, 배당주 중심으로 구성하세요.";
            case "MODERATE" -> "안정과 성장의 균형 잡힌 포트폴리오를 추천합니다. 대형 우량주와 채권을 혼합하세요.";
            case "AGGRESSIVE" -> "적극적인 성장 중심 포트폴리오를 추천합니다. 성장주와 기술주 비중을 높이세요.";
            default -> "포트폴리오를 다양하게 구성하여 리스크를 분산하세요.";
        };
    }

    private String generatePortfolioSuggestion(String riskProfile) {
        return switch (riskProfile) {
            case "CONSERVATIVE" -> "채권 60% / 배당주 25% / 현금 15%";
            case "MODERATE" -> "대형주 40% / 채권 30% / 성장주 20% / 현금 10%";
            case "AGGRESSIVE" -> "성장주 50% / 기술주 30% / 대형주 15% / 현금 5%";
            default -> "인덱스 펀드 50% / 채권 30% / 개별주 20%";
        };
    }

    private SurveySubmitResponse toSubmitResponse(UserSurveyResponse r) {
        return new SurveySubmitResponse(
                r.getResponseId(),
                r.getSurveyId(),
                r.getUserId(),
                r.getStatusCode(),
                r.getTotalScore(),
                r.getRiskProfileCode(),
                r.getCompletedAt(),
                r.getTotalParticipants()
        );
    }

    private SurveyResultResponse toResultResponse(SurveyResult r) {
        return new SurveyResultResponse(
                r.getResultId(),
                r.getUserId(),
                r.getSurveyId(),
                r.getRiskScore(),
                r.getRiskLevelCode(),
                r.getRecommendation(),
                r.getPortfolioSuggestion(),
                r.getAnalyzedAt(),
                r.getValidUntil()
        );
    }

    private SurveyWithMyResponse toSurveyWithMyResponse(SurveyResponse r) {
        return new SurveyWithMyResponse(
            r.getSurveyId(),
            r.getResponseId(),
            r.getSurveyName(),
            r.getDescription(),
            r.getSurveyTypeCode(),
            r.getStatusCode(),
            r.getCompletedAt(), 
            r.getTotalParticipants(),
            r.getRegDt()
        );  
    }

    
}
