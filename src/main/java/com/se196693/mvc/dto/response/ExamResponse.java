package com.se196693.mvc.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamResponse {
    private String title;
    private String description;
}
