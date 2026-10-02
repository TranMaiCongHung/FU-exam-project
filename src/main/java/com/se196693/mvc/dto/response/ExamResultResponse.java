package com.se196693.mvc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamResultResponse {
    private Float score;
    private Integer totalCorrect;
    private Integer totalQuestions;
}
