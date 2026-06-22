package com.investment.survey.application.service.survey;

import com.investment.survey.application.dto.question.*;
import com.investment.survey.application.dto.survey.*;
import com.investment.survey.domain.entity.QuestionWithAnswer;
import com.investment.survey.domain.entity.Survey;
import com.investment.survey.domain.entity.SurveyOption;
import com.investment.survey.domain.entity.SurveyOptionStats;
import com.investment.survey.domain.entity.SurveyQuestion;
import com.investment.survey.domain.repository.SurveyMapper;
import com.investment.survey.domain.repository.SurveyOptionMapper;
import com.investment.survey.domain.repository.SurveyQuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyServiceImpl implements SurveyService {

    private final SurveyMapper surveyMapper;
    private final SurveyQuestionMapper questionMapper;
    private final SurveyOptionMapper optionMapper;

    @Override
    public SurveyResponse getSurvey(Long surveyId) {
        Survey survey = surveyMapper.findSurveyById(surveyId);
        if (survey == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + surveyId);
        }
        return toResponse(survey);
    }

    @Override
    public SurveyDetailResponse getSurveyDetail(Long surveyId) {
        Survey survey = surveyMapper.findSurveyById(surveyId);
        if (survey == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + surveyId);
        }

        List<QuestionResponse> questions = buildQuestionResponses(surveyId);

        return new SurveyDetailResponse(
                survey.getSurveyId(),
                survey.getSurveyName(),
                survey.getDescription(),
                survey.getSurveyTypeCode(),
                survey.getRegDt(),
                survey.getUpdDt(),
                questions
        );
    }

    @Override
    public List<SurveyResponse> getSurveys() {
        return surveyMapper.findAllSurveys().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SurveyResponse> getSurveysByType(String surveyTypeCode) {
        return surveyMapper.findSurveysByTypeCode(surveyTypeCode).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SurveyResponse addSurvey(SurveyAddRequest request) {
        Survey survey = Survey.builder()
                .surveyName(request.surveyName())
                .description(request.description())
                .surveyTypeCode(request.surveyTypeCode())
                .build();

        surveyMapper.insertSurvey(survey);
        log.info("[Survey] 설문 등록 완료 - surveyId: {}", survey.getSurveyId());

        return getSurvey(survey.getSurveyId());
    }

    @Override
    public SurveyResponse modifySurvey(SurveyModRequest request) {
        Survey existing = surveyMapper.findSurveyById(request.surveyId());
        if (existing == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + request.surveyId());
        }

        Survey survey = Survey.builder()
                .surveyId(request.surveyId())
                .surveyName(request.surveyName())
                .description(request.description())
                .surveyTypeCode(request.surveyTypeCode())
                .build();

        surveyMapper.updateSurvey(survey);
        log.info("[Survey] 설문 수정 완료 - surveyId: {}", request.surveyId());

        return getSurvey(request.surveyId());
    }

    @Override
    public void removeSurvey(Long surveyId) {
        Survey existing = surveyMapper.findSurveyById(surveyId);
        if (existing == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + surveyId);
        }

        surveyMapper.deleteSurvey(surveyId);
        log.info("[Survey] 설문 삭제 완료 - surveyId: {}", surveyId);
    }

    @Override
    @Transactional
    public QuestionResponse addQuestion(Long surveyId, QuestionAddRequest request) {
        Survey survey = surveyMapper.findSurveyById(surveyId);
        if (survey == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + surveyId);
        }

        // 문항 등록
        SurveyQuestion question = SurveyQuestion.builder()
                .surveyId(surveyId)
                .questionNumber(request.questionNumber())
                .questionText(request.questionText())
                .questionTypeCode(request.questionTypeCode() != null ? request.questionTypeCode() : "SINGLE_CHOICE")
                .description(request.description())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : request.questionNumber())
                .build();

        questionMapper.insertQuestion(question);

        // 선택지 등록
        if (request.options() != null && !request.options().isEmpty()) {
            for (int i = 0; i < request.options().size(); i++) {
                OptionAddRequest optReq = request.options().get(i);
                SurveyOption option = SurveyOption.builder()
                        .questionId(question.getQuestionId())
                        .optionText(optReq.optionText())
                        .optionValue(optReq.optionValue())
                        .sortOrder(optReq.sortOrder() != null ? optReq.sortOrder() : i + 1)
                        .score(optReq.score())
                        .build();
                optionMapper.insertOption(option);
            }
        }

        log.info("[Survey] 문항 등록 완료 - surveyId: {}, questionId: {}", surveyId, question.getQuestionId());

        return buildSingleQuestionResponse(question.getQuestionId());
    }

    @Override
    public List<QuestionResponse> getQuestions(Long surveyId) {
        return buildQuestionResponses(surveyId);
    }

    @Override
    @Transactional
    public void removeQuestion(Long questionId) {
        SurveyQuestion question = questionMapper.findQuestionById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("문항을 찾을 수 없습니다: " + questionId);
        }

        optionMapper.deleteOptionsByQuestionId(questionId);
        questionMapper.deleteQuestion(questionId);
        log.info("[Survey] 문항 삭제 완료 - questionId: {}", questionId);
    }

    @Override
    public SurveyAnswerResponse getSurveyResponseDetail(Long surveyId, Long responseId, String userId) {
        Survey survey = surveyMapper.findSurveyById(surveyId);
        if (survey == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + surveyId);
        }

        List<QuestionWithAnswerResponse> questionWithAnswers = buildAnswerResponses(surveyId, responseId, userId);

        return new SurveyAnswerResponse(
                survey.getSurveyId(),
                survey.getSurveyName(),
                survey.getDescription(),
                survey.getSurveyTypeCode(),
                survey.getRegDt(),
                survey.getUpdDt(),
                questionWithAnswers
        );
    }

    @Override
    public SurveyOptionStatResponse getSurveyResponseStatsDetail(Long surveyId, Long responseId, String userId) {
        Survey survey = surveyMapper.findSurveyById(surveyId);
        if (survey == null) {
            throw new IllegalArgumentException("설문을 찾을 수 없습니다: " + surveyId);
        }

        List<QuestionWithOptionStatResponse> questionWithAnswers = buildOptionStatResponses(surveyId, responseId, userId);

        return new SurveyOptionStatResponse(
                survey.getSurveyId(),
                survey.getSurveyName(),
                survey.getDescription(),
                survey.getSurveyTypeCode(),
                survey.getRegDt(),
                survey.getUpdDt(),
                questionWithAnswers
        );
    }

    private List<QuestionResponse> buildQuestionResponses(Long surveyId) {
        List<SurveyQuestion> questions = questionMapper.findQuestionsBySurveyId(surveyId);
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream()
                .map(SurveyQuestion::getQuestionId)
                .toList();

        List<SurveyOption> allOptions = optionMapper.findOptionsByQuestionIds(questionIds);
        Map<Long, List<SurveyOption>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(SurveyOption::getQuestionId));

        return questions.stream()
                .map(q -> toQuestionResponse(q, optionsByQuestion.getOrDefault(q.getQuestionId(), Collections.emptyList())))
                .toList();
    }

    private List<QuestionWithAnswerResponse> buildAnswerResponses(Long surveyId, Long responseId, String userId) {
        List<QuestionWithAnswer> questions = questionMapper.findQuestionsWithAnswers(surveyId, responseId, userId);
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream()
                .map(QuestionWithAnswer::getQuestionId)
                .toList();

        List<SurveyOption> allOptions = optionMapper.findOptionsByQuestionIds(questionIds);
        Map<Long, List<SurveyOption>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(SurveyOption::getQuestionId));

        return questions.stream()
                .map(q -> toQuestionWithAnswerResponse(q, optionsByQuestion.getOrDefault(q.getQuestionId(), Collections.emptyList())))
                .toList();
    }

    private List<QuestionWithOptionStatResponse> buildOptionStatResponses(Long surveyId, Long responseId, String userId) {
        List<QuestionWithAnswer> questions = questionMapper.findQuestionsWithAnswers(surveyId, responseId, userId);
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream()
                .map(QuestionWithAnswer::getQuestionId)
                .toList();

        List<SurveyOptionStats> allOptions = optionMapper.findOptionsStatsByQuestionIds(questionIds);
        Map<Long, List<SurveyOptionStats>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(SurveyOptionStats::getQuestionId));

        return questions.stream()
                .map(q -> toQuestionWithOptionStatResponse(q, optionsByQuestion.getOrDefault(q.getQuestionId(), Collections.emptyList())))
                .toList();
    }

    

    private QuestionResponse buildSingleQuestionResponse(Long questionId) {
        SurveyQuestion question = questionMapper.findQuestionById(questionId);
        List<SurveyOption> options = optionMapper.findOptionsByQuestionId(questionId);
        return toQuestionResponse(question, options);
    }

    private SurveyResponse toResponse(Survey s) {
        return new SurveyResponse(
                s.getSurveyId(),
                s.getSurveyName(),
                s.getDescription(),
                s.getSurveyTypeCode(),
                s.getRegDt(),
                s.getUpdDt()
        );
    }

    

    private QuestionResponse toQuestionResponse(SurveyQuestion q, List<SurveyOption> options) {
        List<OptionResponse> optionResponses = options.stream()
                .map(o -> new OptionResponse(
                        o.getOptionId(),
                        o.getQuestionId(),
                        o.getOptionText(),
                        o.getOptionValue(),
                        o.getSortOrder(),
                        o.getScore()
                ))
                .toList();

        return new QuestionResponse(
                q.getQuestionId(),
                q.getSurveyId(),
                q.getQuestionNumber(),
                q.getQuestionText(),
                q.getQuestionTypeCode(),
                q.getDescription(),
                q.getSortOrder(),
                optionResponses
        );
    }

    private QuestionWithAnswerResponse toQuestionWithAnswerResponse(QuestionWithAnswer q, List<SurveyOption> options) {
        List<OptionResponse> optionResponses = options.stream()
                .map(o -> new OptionResponse(
                        o.getOptionId(),
                        o.getQuestionId(),
                        o.getOptionText(),
                        o.getOptionValue(),
                        o.getSortOrder(),
                        o.getScore()
                ))
                .toList();

        return new QuestionWithAnswerResponse(
                q.getQuestionId(),
                q.getSurveyId(),
                q.getQuestionNumber(),
                q.getQuestionText(),
                q.getQuestionTypeCode(),
                q.getDescription(),
                q.getSortOrder(),
                q.getSelectedOptionId(),
                q.getSelectedValue(),
                optionResponses
        );
    }

    private QuestionWithOptionStatResponse toQuestionWithOptionStatResponse(QuestionWithAnswer q, List<SurveyOptionStats> options) {
        List<OptionStatsResponse> optionResponses = options.stream()
                .map(o -> new OptionStatsResponse(
                        o.getOptionId(),
                        o.getQuestionId(),
                        o.getOptionText(),
                        o.getOptionValue(),
                        o.getSortOrder(),
                        o.getScore(),
                        o.getRegDt(),
                        o.getSelectedCount()
                ))
                .toList();

        return new QuestionWithOptionStatResponse(
                q.getQuestionId(),
                q.getSurveyId(),
                q.getQuestionNumber(),
                q.getQuestionText(),
                q.getQuestionTypeCode(),
                q.getDescription(),
                q.getSortOrder(),
                q.getSelectedOptionId(),
                q.getSelectedValue(),
                optionResponses
        );
    }

    

    
}
