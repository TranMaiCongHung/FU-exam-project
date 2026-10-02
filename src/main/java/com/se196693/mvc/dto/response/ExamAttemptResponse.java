package com.se196693.mvc.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ExamAttemptResponse {
    private Long attemptId;
    private LocalDateTime startedAt;
}
