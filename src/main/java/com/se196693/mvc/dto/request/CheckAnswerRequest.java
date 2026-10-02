package com.se196693.mvc.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CheckAnswerRequest {
    private List<Long> selectedOptionIds;
}
