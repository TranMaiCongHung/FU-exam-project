package com.se196693.mvc.dto.response;

import com.se196693.mvc.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResponse {
    Long id;
    Integer questionNumber;
    String imageUrl;
    QuestionType questionType;
}
