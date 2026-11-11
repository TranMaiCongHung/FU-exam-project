package com.se196693.mvc.dto.response;

import com.se196693.mvc.enums.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionReviewResponse {
    private Long questionId;
    private Integer questionNumber;
    private String content;
    private String imageUrl;
    private QuestionType questionType;
    private List<AnswerOptionResponse> answerOptions; // Danh sách A, B, C, D
    private List<Long> userSelectedOptionIds;        // Đáp án người dùng đã chọn
    private List<Long> correctOptionIds;             // Đáp án chính xác của hệ thống
    private Boolean isCorrect;                       // Đúng (true), Sai (false), Chưa làm (null hoặc false)
}