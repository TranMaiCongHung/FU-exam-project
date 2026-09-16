package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.QuestionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class QuestionRequest {
    @NotNull
    private Integer questionNumber;

    private MultipartFile image;

    @NotNull
    private String content;

    @NotNull
    private QuestionType questionType;

    private List<AnswerOptionRequest> answerOption;
}
