package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.CheckAnswerResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {
    QuestionResponse addQuestion(Long examId,
                                 QuestionRequest request);

    List<QuestionResponse> getQuestions(Long examId);
    CheckAnswerResponse checkAnswer(Long examId, Long questionId, CheckAnswerRequest request);

}
