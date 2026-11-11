package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.request.SubmitExamRequest;
import com.se196693.mvc.dto.response.*;
import com.se196693.mvc.entity.*;
import com.se196693.mvc.enums.ExamStatus;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.*;
import com.se196693.mvc.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PracticeServiceImpl implements PracticeService {

    private final ExamAttemptRepository examAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    @Override
    public ExamAttemptResponse startPractice(Long examId, String username) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Exam not found");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ExamAttempt attempt = ExamAttempt.builder()
                .exam(exam)
                .user(user)
                .totalQuestions(exam.getQuestions().size())
                .totalCorrect(0)
                .score(0f)
                .startedAt(LocalDateTime.now())
                .build();

        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        return ExamAttemptResponse.builder()
                .attemptId(savedAttempt.getId())
                .startedAt(savedAttempt.getStartedAt())
                .build();
    }

    @Override
    public CheckAnswerResponse checkAndSaveAnswer(Long attemptId, Long questionId, CheckAnswerRequest request) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        List<Long> correctOptionIds = question.getAnswerOptions().stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId)
                .toList();

        List<Long> selectedIds = request.getSelectedOptionIds() != null
                ? request.getSelectedOptionIds() : java.util.Collections.emptyList();

        boolean isUserCorrect = correctOptionIds.size() == selectedIds.size() &&
                correctOptionIds.containsAll(selectedIds);

        String selectedIdsStr = selectedIds.stream()
                .map(String::valueOf).collect(Collectors.joining(","));

        UserAnswer answer = UserAnswer.builder()
                .examAttempt(attempt)
                .question(question)
                .selectedOptionIds(selectedIdsStr)
                .isCorrect(isUserCorrect)
                .build();
        userAnswerRepository.save(answer);

        return CheckAnswerResponse.builder()
                .isCorrect(isUserCorrect)
                .correctOptionIds(correctOptionIds)
                .build();
    }

    @Override
    public ExamResultResponse finishPractice(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        int correctCount = userAnswerRepository.countByExamAttemptIdAndIsCorrectTrue(attemptId);
        attempt.setTotalCorrect(correctCount);

        float score = (float) correctCount / attempt.getTotalQuestions() * 10;
        attempt.setScore((float) (Math.round(score * 100.0) / 100.0));
        attempt.setFinishedAt(LocalDateTime.now());

        examAttemptRepository.save(attempt);

        return ExamResultResponse.builder()
                .score(attempt.getScore())
                .totalCorrect(attempt.getTotalCorrect())
                .totalQuestions(attempt.getTotalQuestions())
                .build();
    }

    @org.springframework.beans.factory.annotation.Value("${r2.public-url}")
    private String publicUrl;

    @Override
    public ExamReviewResponse submitAndFinishPractice(Long attemptId, SubmitExamRequest request) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        Exam exam = attempt.getExam();
        List<Question> questions = exam.getQuestions();

        int correctCount = 0;

        // Lưu câu trả lời của từng câu
        if (request != null && request.getAnswers() != null) {
            for (Question question : questions) {
                List<Long> selectedIds = request.getAnswers().getOrDefault(question.getId(), List.of());

                List<Long> correctOptionIds = question.getAnswerOptions().stream()
                        .filter(AnswerOption::isCorrect)
                        .map(AnswerOption::getId)
                        .toList();

                boolean isUserCorrect = !selectedIds.isEmpty()
                        && correctOptionIds.size() == selectedIds.size()
                        && correctOptionIds.containsAll(selectedIds);

                if (isUserCorrect) correctCount++;

                String selectedIdsStr = selectedIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                // Cập nhật câu trả lời nếu đã tồn tại, hoặc tạo mới
                UserAnswer userAnswer = userAnswerRepository.findByExamAttemptIdAndQuestionId(attemptId, question.getId())
                        .orElse(UserAnswer.builder()
                                .examAttempt(attempt)
                                .question(question)
                                .build());

                userAnswer.setSelectedOptionIds(selectedIdsStr);
                userAnswer.setCorrect(isUserCorrect);
                userAnswerRepository.save(userAnswer);
            }
        }

        // Cập nhật điểm cho lượt thi
        attempt.setTotalCorrect(correctCount);
        float score = attempt.getTotalQuestions() > 0 ? (float) correctCount / attempt.getTotalQuestions() * 10 : 0f;
        attempt.setScore((float) (Math.round(score * 100.0) / 100.0));
        attempt.setFinishedAt(LocalDateTime.now());
        examAttemptRepository.save(attempt);

        // Trả về toàn bộ chi tiết bài làm để người dùng xem lại
        return getAttemptReview(attemptId);
    }

    @Override
    public ExamReviewResponse getAttemptReview(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        Exam exam = attempt.getExam();
        List<UserAnswer> userAnswers = userAnswerRepository.findByExamAttemptId(attemptId);

        // Map nhanh questionId -> UserAnswer
        java.util.Map<Long, UserAnswer> answerMap = userAnswers.stream()
                .collect(Collectors.toMap(ua -> ua.getQuestion().getId(), ua -> ua, (existing, replacement) -> existing));

        List<QuestionReviewResponse> questionReviews = exam.getQuestions().stream()
                .sorted(java.util.Comparator.comparing(Question::getQuestionNumber))
                .map(q -> {
                    UserAnswer ua = answerMap.get(q.getId());

                    List<Long> selectedIds = java.util.Collections.emptyList();
                    boolean isCorrect = false;

                    if (ua != null) {
                        isCorrect = ua.isCorrect();
                        if (ua.getSelectedOptionIds() != null && !ua.getSelectedOptionIds().isBlank()) {
                            selectedIds = java.util.Arrays.stream(ua.getSelectedOptionIds().split(","))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .map(Long::parseLong)
                                    .toList();
                        }
                    }

                    List<Long> correctOptionIds = q.getAnswerOptions().stream()
                            .filter(AnswerOption::isCorrect)
                            .map(AnswerOption::getId)
                            .toList();

                    List<AnswerOptionResponse> options = q.getAnswerOptions().stream()
                            .map(opt -> AnswerOptionResponse.builder()
                                    .id(opt.getId())
                                    .optionLabel(opt.getOptionLabel())
                                    .content(opt.getContent())
                                    .isCorrect(opt.isCorrect())
                                    .build())
                            .toList();

                    String fullImageUrl = q.getObjectKey() != null
                            ? publicUrl.replaceAll("/+$", "") + "/" + q.getObjectKey()
                            : null;

                    return QuestionReviewResponse.builder()
                            .questionId(q.getId())
                            .questionNumber(q.getQuestionNumber())
                            .content(q.getContent())
                            .imageUrl(fullImageUrl)
                            .questionType(q.getQuestionType())
                            .answerOptions(options)
                            .userSelectedOptionIds(selectedIds)
                            .correctOptionIds(correctOptionIds)
                            .isCorrect(isCorrect)
                            .build();
                })
                .toList();

        return ExamReviewResponse.builder()
                .attemptId(attempt.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .score(attempt.getScore())
                .totalCorrect(attempt.getTotalCorrect())
                .totalQuestions(attempt.getTotalQuestions())
                .startedAt(attempt.getStartedAt())
                .finishedAt(attempt.getFinishedAt())
                .questions(questionReviews)
                .build();
    }
}
