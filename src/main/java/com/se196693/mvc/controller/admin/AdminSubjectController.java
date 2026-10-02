package com.se196693.mvc.controller.admin;

import com.se196693.mvc.dto.request.SubjectRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.SubjectResponse;
import com.se196693.mvc.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@PreAuthorize(("hasRole('ADMIN')"))
@RequiredArgsConstructor
public class AdminSubjectController {
    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(@Valid @RequestBody SubjectRequest subjectRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "Created successfully",
                        subjectService.createSubject(subjectRequest)
                )
        );
    }

    @GetMapping("/{termNumber}")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSubjectsByTermNumber(@PathVariable Integer termNumber) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Fetched successfully",
                        subjectService.getSubjectsByTermNumber(termNumber)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest subjectRequest) {
        return ResponseEntity.ok(
                ApiResponse.success(
                "Updated successfully",
                subjectService.updateSubject(id, subjectRequest)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(
                ApiResponse.success("Deleted successfully",null) // Hoặc ApiResponse.success("Deleted successfully", null)
        );
    }
}
