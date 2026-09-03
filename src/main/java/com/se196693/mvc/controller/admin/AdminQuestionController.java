package com.se196693.mvc.controller.admin;

import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionController {
    private final QuestionService questionService;

    @PostMapping(value = "/{examId}/quesions",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable Long examId,
            @ModelAttribute QuestionRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "Question created successfully",
                        questionService.addQuestion(examId, request))
        );
    }
}
