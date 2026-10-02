package com.se196693.mvc.controller.user;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.CheckAnswerResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions(@PathVariable Long examId){
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Fetched exam successfully",
                        questionService.getQuestions(examId))
        );
    }

    @PostMapping("/{examId}/questions/{questionId}/check")
    public ResponseEntity<ApiResponse<CheckAnswerResponse>> checkAnswer(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestBody CheckAnswerRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Answer checked successfully",
                        questionService.checkAnswer(examId, questionId, request)
                )
        );
    }

}
