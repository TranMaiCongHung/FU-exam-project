package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.QuestionRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OcrService {
    List<QuestionRequest> extractQuestionsFromImage(List<MultipartFile> file);
}
