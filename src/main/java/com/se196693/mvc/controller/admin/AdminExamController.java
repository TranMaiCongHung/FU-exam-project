package com.se196693.mvc.controller.admin;

import com.se196693.mvc.dto.request.ExamFilterRequest;
import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.ExamResponse;
import com.se196693.mvc.dto.response.PageResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.service.ExamService;
import com.se196693.mvc.service.OcrService;
import com.se196693.mvc.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExamController {
    private final ExamService examService;

    private final OcrService ocrService;
    private final QuestionService questionService;

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

    @PostMapping(value = "/{examId}/extract-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<QuestionRequest>>> extractQuestionViaOcr(
            @PathVariable Long examId,
            @RequestParam("file") List<MultipartFile> document) {

        List<QuestionRequest> extractedQuestions = examService.extractOcr(examId, document);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ApiResponse.success(
                        "Quét OCR thành công! Vui lòng duyệt lại trước khi lưu.",
                        extractedQuestions
                )
        );
    }

    @PostMapping(value = "/{examId}/questions/bulk")
    public ResponseEntity<ApiResponse<String>> saveBulkQuestions(
            @PathVariable Long examId,
            @RequestBody List<QuestionRequest> questions) {
        questionService.saveOcrQuestions(examId, questions);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Đã lưu toàn bộ câu hỏi vào hệ thống thành công!",
                        null
                )
        );
    }
}
