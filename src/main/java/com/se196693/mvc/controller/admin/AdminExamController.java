package com.se196693.mvc.controller.admin;

import com.se196693.mvc.dto.request.ExamFilterRequest;
import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.ExamResponse;
import com.se196693.mvc.dto.response.PageResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExamController {
    private final ExamService examService;

    @PostMapping("/{subjectId}")
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(
            @PathVariable Long subjectId,
            @Valid @RequestBody ExamRequest request, Authentication authentication
            ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "Exam created successfully",
                        examService.createExam(subjectId, request, authentication.getName()))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExamResponse>>> getExams(@ParameterObject @ModelAttribute ExamFilterRequest filter,
                                                                            @ParameterObject Pageable pageable){
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listed exams successfully",
                        examService.getExams(filter, pageable)
                )
        );
    }
}
