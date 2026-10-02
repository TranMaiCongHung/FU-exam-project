package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.response.*;
import com.se196693.mvc.entity.*;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.*;
import com.se196693.mvc.service.PracticeService;
import lombok.RequiredArgsConstructor;
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
}
