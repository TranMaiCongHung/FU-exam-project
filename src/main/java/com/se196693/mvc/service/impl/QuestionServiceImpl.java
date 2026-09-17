package com.se196693.mvc.service.impl;

import java.util.List;
import java.util.UUID;

import com.se196693.mvc.dto.request.UpdateQuestionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.AnswerOptionResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.entity.AnswerOption;
import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.entity.Question;
import com.se196693.mvc.enums.ExamStatus;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.ExamRepository;
import com.se196693.mvc.repository.QuestionRepository;
import com.se196693.mvc.service.FileStorageService;
import com.se196693.mvc.service.QuestionService;
import com.se196693.mvc.utils.FileUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final FileStorageService fileStorageService;

    @Value("${r2.public-url}")
    private String publicUrl;

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

    @Override
    public void saveOcrQuestions(Long examId, List<QuestionRequest> ocrQuestions) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        for (QuestionRequest req : ocrQuestions) {
            Question question = Question.builder()
                    .questionNumber(req.getQuestionNumber())
                    .content(req.getContent())
                    .objectKey(null) // Lặp lại: Vì OCR bóc chữ nên không có ảnh cắt lẻ từng câu
                    .questionType(req.getQuestionType())
                    .exam(exam)
                    .build();
            if (req.getAnswerOption() != null && !req.getAnswerOption().isEmpty()) {
                List<AnswerOption> options = req.getAnswerOption().stream()
                        .map(optReq -> AnswerOption.builder()
                        .optionLabel(optReq.getOptionLabel())
                        .content(optReq.getContent())
                        .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                        .question(question)
                        .build())
                        .toList();
                question.setAnswerOptions(options);
            }
            questionRepository.save(question);
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        examRepository.save(exam);

    }

    @Override
    public QuestionResponse uploadQuestionImage(Long examId, Long questionId, MultipartFile image) {
        // 1. Kiểm tra bài thi và câu hỏi có tồn tại không
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!question.getExam().getId().equals(examId)) {
            throw new IllegalArgumentException("Question does not belong to this exam");
        }

        // 2. Kiểm tra file ảnh hợp lệ
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is empty");
        }

        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG and WEBP images are allowed");
        }

        // 3. Đẩy ảnh lên Cloudflare R2
        String extension = FileUtils.getExtension(image.getOriginalFilename());
        String objectKey = "exams/" + examId + "/questions/" + question.getQuestionNumber() + "-" + UUID.randomUUID() + "." + extension;

        fileStorageService.upload(image, objectKey);

        // 4. Lưu link ảnh vào Database
        question.setObjectKey(objectKey);
        Question savedQuestion = questionRepository.save(question);

        return convertToQuestion(savedQuestion);
    }

    @Override
    public QuestionResponse updateQuestion(Long examId, Long questionId, UpdateQuestionRequest request) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        if (!question.getExam().getId().equals(examId)) {
            throw new IllegalArgumentException("Question does not belong to this exam");
        }

        question.setQuestionNumber(request.getQuestionNumber());
        question.setContent(request.getContent());
        question.setQuestionType(request.getQuestionType());

        question.getAnswerOptions().clear();
        if (request.getAnswerOption() != null && !request.getAnswerOption().isEmpty()) {
            List<AnswerOption> updatedOptions = request.getAnswerOption().stream()
                    .map(optReq -> AnswerOption.builder()
                            .optionLabel(optReq.getOptionLabel())
                            .content(optReq.getContent())
                            .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                            .question(question)
                            .build())
                    .toList();
            question.getAnswerOptions().addAll(updatedOptions);
        }
        Question savedQuestion = questionRepository.save(question);
        return convertToQuestion(savedQuestion);
    }

    @Override
    public void deleteQuestion(Long examId, Long questionId) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        if (!question.getExam().getId().equals(examId)) {
            throw new IllegalArgumentException("Question does not belong to this exam");
        }
        if (question.getObjectKey() != null) {
            fileStorageService.delete(question.getObjectKey());
        }
        questionRepository.delete(question);
    }


    private QuestionResponse convertToQuestion(Question question) {
        List<AnswerOptionResponse> optionResponses = null;
        if (question.getAnswerOptions() != null && !question.getAnswerOptions().isEmpty()) {
            optionResponses = question.getAnswerOptions().stream().map(opt
                    -> AnswerOptionResponse.builder()
                            .id(opt.getId())
                            .optionLabel(opt.getOptionLabel())
                            .content(opt.getContent())
                            .isCorrect(opt.isCorrect())
                            .build()
            ).toList();
        }
        return QuestionResponse.builder()
                .id(question.getId())
                .questionNumber(question.getQuestionNumber())
                .imageUrl(publicUrl + "/" + question.getObjectKey())
                .questionType(question.getQuestionType())
                .answerOption(optionResponses)
                .build();
    }
}
