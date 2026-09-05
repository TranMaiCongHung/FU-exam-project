package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ExamRequest {
    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "Exam type is required")
    private ExamType examType;

    @NotNull(message = "semester code is required")
    @Pattern(
            regexp = "^(FA||SU||SP)\\d{2}",
            message = "Semester code must be formatted SP/SU/FA + 2 digits of year (e.g: SU26)"
    )
    private String semesterCode;
}
