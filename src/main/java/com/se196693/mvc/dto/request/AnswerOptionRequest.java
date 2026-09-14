package com.se196693.mvc.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerOptionRequest {
    private String optionLabel;
    private String content;
    private boolean isCorrect;
}
