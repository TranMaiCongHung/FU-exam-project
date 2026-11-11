package com.se196693.mvc.dto.response;

import com.se196693.mvc.enums.ExamStatus;
import com.se196693.mvc.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamResponse {
    private Long id;
    private String title;
    private String description;
    private ExamType examType;
    private String semesterCode;
    private LocalDateTime createdAt;
    private ExamStatus status;
}
