package com.se196693.mvc.service;

import java.util.List;

import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.request.UpdateQuestionRequest;
import com.se196693.mvc.dto.response.QuestionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {

    List<QuestionResponse> getQuestions(Long examId);

    void saveOcrQuestions(Long examId, List<QuestionRequest> ocrQuestions);

    QuestionResponse uploadQuestionImage(Long examId, Long questionId, MultipartFile image);

    QuestionResponse updateQuestion(Long examId, Long questionId, UpdateQuestionRequest request);

    void deleteQuestion(Long examId, Long questionId);

}
