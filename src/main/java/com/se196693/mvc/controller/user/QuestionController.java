package com.se196693.mvc.controller.user;

import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/exams")
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

}
