package com.se196693.mvc.controller.admin;

import com.se196693.mvc.dto.request.UpdateQuestionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.service.QuestionService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionController {

    private final QuestionService questionService;

//    @PostMapping(value = "/{examId}/questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
//            @PathVariable Long examId,
//            @ModelAttribute QuestionRequest request
//    ) {
//        return ResponseEntity.status(HttpStatus.CREATED).body(
//                ApiResponse.created(
//                        "Question created successfully",
//                        questionService.addQuestion(examId, request))
//        );
//    }

    @PatchMapping(value = "/{examId}/questions/{questionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionResponse>> uploadQuestionImage(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                ApiResponse.success("Image uploaded successfully",
                        questionService.uploadQuestionImage(examId, questionId, file))
        );
    }

    @PutMapping(value = "/{examId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Question updated successfully",
                        questionService.updateQuestion(examId, questionId, request))
        );
    }
    @DeleteMapping(value = "/{examId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId
    ) {
        questionService.deleteQuestion(examId, questionId);
        return ResponseEntity.ok(
                ApiResponse.success("Question deleted successfully", null)
        );
    }

}
