package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.CheckAnswerResponse;
import com.se196693.mvc.dto.request.UpdateQuestionRequest;
import com.se196693.mvc.dto.response.QuestionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {
    QuestionResponse addQuestion(Long examId, QuestionRequest request);

    List<QuestionResponse> getQuestions(Long examId);

    void saveOcrQuestions(Long examId, List<QuestionRequest> ocrQuestions);

    QuestionResponse uploadQuestionImage(Long examId, Long questionId, MultipartFile image);

    QuestionResponse updateQuestion(Long examId, Long questionId, UpdateQuestionRequest request);

    void deleteQuestion(Long examId, Long questionId);

    CheckAnswerResponse checkAnswer(Long examId, Long questionId, CheckAnswerRequest request);

}
