package com.se196693.mvc.controller.user;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.response.*;
import com.se196693.mvc.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')") // Chỉ user đã đăng nhập mới được thi
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/exams/{examId}/start")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> startPractice(
            @PathVariable Long examId,
            Authentication authentication) {

        // authentication.getName() sẽ lấy ra username của người đang đăng nhập
        return ResponseEntity.ok(ApiResponse.success(
                "Started practice session",
                practiceService.startPractice(examId, authentication.getName())
        ));
    }

    @PostMapping("/attempts/{attemptId}/questions/{questionId}/check")
    public ResponseEntity<ApiResponse<CheckAnswerResponse>> checkAnswer(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @RequestBody CheckAnswerRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Answer saved",
                practiceService.checkAndSaveAnswer(attemptId, questionId, request)
        ));
    }

    @PostMapping("/attempts/{attemptId}/finish")
    public ResponseEntity<ApiResponse<ExamResultResponse>> finishPractice(
            @PathVariable Long attemptId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Practice finished",
                practiceService.finishPractice(attemptId)
        ));
    }
}
