package com.se196693.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnswerOptionResponse {
    private Long id;
    private String optionLabel;
    private String content;
    private boolean isCorrect;
}
