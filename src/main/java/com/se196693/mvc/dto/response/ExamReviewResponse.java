package com.se196693.mvc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamReviewResponse {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private Float score;
    private Integer totalCorrect;
    private Integer totalQuestions;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<QuestionReviewResponse> questions; // Chi tiết từng câu
}