package com.se196693.mvc.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CheckAnswerResponse {
    private boolean isCorrect;
    private List<Long> correctOptionIds;
}
