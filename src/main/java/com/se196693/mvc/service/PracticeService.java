package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.response.CheckAnswerResponse;
import com.se196693.mvc.dto.response.ExamAttemptResponse;
import com.se196693.mvc.dto.response.ExamResultResponse;

public interface PracticeService {
    ExamAttemptResponse startPractice(Long examId, String username);
    CheckAnswerResponse checkAndSaveAnswer(Long attemptId, Long questionId, CheckAnswerRequest request);
    ExamResultResponse finishPractice(Long attemptId);
}
