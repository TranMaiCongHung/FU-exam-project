package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.entity.Question;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.ExamRepository;
import com.se196693.mvc.repository.QuestionRepository;
import com.se196693.mvc.service.FileStorageService;
import com.se196693.mvc.service.QuestionService;
import com.se196693.mvc.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final FileStorageService fileStorageService;
    @Override
    public QuestionResponse addQuestion(Long examId, QuestionRequest request) {
        Exam exam = examRepository.findById(examId).orElseThrow(
                () -> new ResourceNotFoundException("Exam with id: " + examId + " not found")
        );

        MultipartFile image = request.getImage();

        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image is empty");
        }

        String contentType = image.getContentType();

        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {

            throw new IllegalArgumentException(
                    "Only JPEG, PNG and WEBP images are allowed"
            );
        }

        String extension = FileUtils.getExtension(image.getOriginalFilename());

        String objectKey = "exams/"
                + examId
                + "/questions/"
                + request.getQuestionNumber()
                + "-"
                + UUID.randomUUID()
                + "."
                + extension;
        String imageUrl = fileStorageService.upload(image, objectKey);

        Question savedQuestion = questionRepository.save(
                Question.builder()
                        .questionNumber(request.getQuestionNumber())
                        .imageUrl(imageUrl)
                        .objectKey(objectKey)
                        .exam(exam)
                        .build()
        );
        return convertToQuestion(savedQuestion);
    }

    @Override
    public List<QuestionResponse> getQuestions(Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException(
                    "Exam not found"
            );
        }

        List<Question> questions = questionRepository.findByExamIdOrderByQuestionNumberAsc(examId);

        return questions.stream().map(this::convertToQuestion).toList();
    }

    private QuestionResponse convertToQuestion(Question question){
        return QuestionResponse.builder()
                .id(question.getId())
                .imageUrl(question.getImageUrl())
                .questionNumber(question.getQuestionNumber())
                .build();
    }
}
