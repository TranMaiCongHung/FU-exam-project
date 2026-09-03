package com.se196693.mvc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamRequest {
    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "description is required")
    private String description;
}
