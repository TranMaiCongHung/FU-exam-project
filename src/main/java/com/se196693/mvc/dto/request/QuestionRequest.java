package com.se196693.mvc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class QuestionRequest {
    @NotNull
    private Integer questionNumber;

    @NotNull
    private MultipartFile image;
}
