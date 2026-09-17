package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.QuestionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateQuestionRequest {
    @NotNull
    private Integer questionNumber;

    @NotNull
    private String content;

    @NotNull
    private QuestionType questionType;

    private List<AnswerOptionRequest> answerOption;
}
