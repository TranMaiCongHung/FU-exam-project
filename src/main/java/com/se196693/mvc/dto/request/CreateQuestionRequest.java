package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuestionRequest {
    private Integer questionNumber;

    private String content;

    @NotNull
    private QuestionType questionType;

    @NotEmpty
    @Valid
    private List<AnswerOptionRequest> answerOption;
}
