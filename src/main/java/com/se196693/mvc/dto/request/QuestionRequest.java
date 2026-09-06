package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.QuestionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class QuestionRequest {
    @NotNull
    private Integer questionNumber;

    @NotNull
    private MultipartFile image;

    @NotNull
    private QuestionType questionType;
}
