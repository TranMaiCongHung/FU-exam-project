package com.se196693.mvc.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.se196693.mvc.dto.request.CheckAnswerRequest;
import com.se196693.mvc.dto.request.AnswerOptionRequest;
import com.se196693.mvc.dto.request.CreateQuestionRequest;
import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.request.UpdateQuestionRequest;
import com.se196693.mvc.dto.response.AnswerOptionResponse;
import com.se196693.mvc.dto.response.CheckAnswerResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.entity.AnswerOption;
import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.entity.Question;
import com.se196693.mvc.enums.ExamStatus;
import com.se196693.mvc.enums.QuestionType;
import com.se196693.mvc.exception.DuplicateResourceException;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.ExamRepository;
import com.se196693.mvc.repository.QuestionRepository;
import com.se196693.mvc.service.FileStorageService;
import com.se196693.mvc.service.QuestionService;
import com.se196693.mvc.utils.FileUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final FileStorageService fileStorageService;

    @Value("${r2.public-url}")
    private String publicUrl;

    @Transactional
    @Override
    public QuestionResponse addQuestion(Long examId, CreateQuestionRequest request, MultipartFile image) {
        Exam exam = examRepository.findById(examId).orElseThrow(
                () -> new ResourceNotFoundException("Exam with id: " + examId + " not found")
        );

        boolean hasImage = image != null && !image.isEmpty();
        if (hasImage) {
            validateImage(image);
        }

        String content = request.getContent();
        boolean hasContent = content != null && !content.isBlank();
        if (!hasContent && !hasImage) {
            throw new IllegalArgumentException("Question must have content or image");
        }

        validateAnswerOptions(request.getQuestionType(), request.getAnswerOption());

        Integer questionNumber = request.getQuestionNumber();
        if (questionNumber == null) {
            questionNumber = questionRepository.findMaxQuestionNumberByExamId(examId) + 1;
        } else if (questionRepository.existsByExamIdAndQuestionNumber(examId, questionNumber)) {
            throw new DuplicateResourceException(
                    "Question number " + questionNumber + " already exists in this exam");
        }

        String objectKey = null;
        if (hasImage) {
            objectKey = buildObjectKey(examId, questionNumber, image.getOriginalFilename());
            fileStorageService.upload(image, objectKey);
        }

        Question question = Question.builder()
                .questionNumber(questionNumber)
                .content(hasContent ? content.trim() : null)
                .objectKey(objectKey)
                .exam(exam)
                .questionType(request.getQuestionType())
                .build();
        question.setAnswerOptions(buildOptions(request.getAnswerOption(), question));

        try {
            Question savedQuestion = questionRepository.saveAndFlush(question);
            return convertToQuestion(savedQuestion);
        } catch (RuntimeException e) {
            if (objectKey != null) {
                try {
                    fileStorageService.delete(objectKey);
                } catch (Exception deleteError) {
                    log.warn("Failed to clean up uploaded image {}: {}", objectKey, deleteError.getMessage());
                }
            }
            throw e;
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is empty");
        }
        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG and WEBP images are allowed");
        }
    }

    private String buildObjectKey(Long examId, Integer questionNumber, String originalFilename) {
        String extension = FileUtils.getExtension(originalFilename);
        return "exams/" + examId + "/questions/" + questionNumber + "-" + UUID.randomUUID() + "." + extension;
    }

    private void validateAnswerOptions(QuestionType type, List<AnswerOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("Question must have at least 2 answer options");
        }
        Set<String> labels = new HashSet<>();
        long correctCount = 0;
        for (AnswerOptionRequest opt : options) {
            if (opt.getOptionLabel() == null || opt.getOptionLabel().isBlank()) {
                throw new IllegalArgumentException("Answer option label must not be blank");
            }
            if (!labels.add(opt.getOptionLabel().trim().toLowerCase())) {
                throw new IllegalArgumentException("Duplicate answer option label: " + opt.getOptionLabel().trim());
            }
            if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                correctCount++;
            }
        }
        if (type == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new IllegalArgumentException("Single choice question must have exactly 1 correct answer");
        }
        if (type == QuestionType.MULTIPLE_CHOICE && correctCount < 1) {
            throw new IllegalArgumentException("Multiple choice question must have at least 1 correct answer");
        }
    }

    private List<AnswerOption> buildOptions(List<AnswerOptionRequest> requests, Question question) {
        List<AnswerOption> result = new ArrayList<>();
        if (requests == null) {
            return result;
        }
        for (AnswerOptionRequest opt : requests) {
            result.add(AnswerOption.builder()
                    .optionLabel(opt.getOptionLabel())
                    .content(opt.getContent())
                    .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                    .question(question)
                    .build());
        }
        return result;
    }

    @Override
    public List<QuestionResponse> getQuestions(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Exam not found");
        }

        List<Question> questions = questionRepository.findByExamIdOrderByQuestionNumberAsc(examId);

        return questions.stream().map(this::convertToQuestion).toList();
    }

    @Transactional
    @Override
    public void saveOcrQuestions(Long examId, List<QuestionRequest> ocrQuestions) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        if (ocrQuestions == null || ocrQuestions.isEmpty()) {
            throw new IllegalArgumentException("Danh sách câu hỏi không được để trống");
        }

        // 1. Append-only + tự đánh số lại: giữ số câu OCR nếu hợp lệ (>0), chưa có trong DB
        //    và chưa xuất hiện trước đó trong request; còn lại gán tiếp từ số lớn nhất + 1.
        Set<Integer> requested = new HashSet<>();
        for (QuestionRequest req : ocrQuestions) {
            if (req.getQuestionNumber() != null && req.getQuestionNumber() > 0) {
                requested.add(req.getQuestionNumber());
            }
        }
        Set<Integer> taken = requested.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(questionRepository.findExistingQuestionNumbers(examId, requested));

        List<Integer> assigned = new ArrayList<>(ocrQuestions.size());
        int maxNumber = questionRepository.findMaxQuestionNumberByExamId(examId);
        for (QuestionRequest req : ocrQuestions) {
            Integer number = req.getQuestionNumber();
            if (number != null && number > 0 && taken.add(number)) {
                assigned.add(number);
                maxNumber = Math.max(maxNumber, number);
            } else {
                assigned.add(null);
            }
        }
        for (int i = 0; i < assigned.size(); i++) {
            if (assigned.get(i) == null) {
                assigned.set(i, ++maxNumber);
            }
        }

        // 2. CHỐNG LỆCH ẢNH: Gán đúng objectKey của từng câu hỏi
        for (int i = 0; i < ocrQuestions.size(); i++) {
            QuestionRequest req = ocrQuestions.get(i);
            String objectKey = req.getObjectKey();

            // Cơ chế phòng thủ: nếu FE chỉ gửi imageUrl (hoặc khi load từ DB lên rồi save lại)
            if ((objectKey == null || objectKey.isBlank()) && req.getImageUrl() != null) {
                objectKey = extractKeyFromImageUrl(req.getImageUrl());
            }

            Question question = Question.builder()
                    .questionNumber(assigned.get(i))
                    .content(req.getContent())
                    .objectKey(objectKey)
                    .questionType(req.getQuestionType())
                    .exam(exam)
                    .build();

            if (req.getAnswerOption() != null && !req.getAnswerOption().isEmpty()) {
                question.setAnswerOptions(buildOptions(req.getAnswerOption(), question));
            }
            questionRepository.save(question);
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        examRepository.save(exam);
    }

    // Hàm phụ trợ: Bóc tách objectKey từ URL CDN nếu cần
    private String extractKeyFromImageUrl(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.contains("exams/")) {
            return url.substring(url.indexOf("exams/"));
        }
        return url;
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
        validateImage(image);

        // 3. Đẩy ảnh lên Cloudflare R2
        String objectKey = buildObjectKey(examId, question.getQuestionNumber(), image.getOriginalFilename());

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

        validateAnswerOptions(request.getQuestionType(), request.getAnswerOption());

        if (!java.util.Objects.equals(question.getQuestionNumber(), request.getQuestionNumber())
                && questionRepository.existsByExamIdAndQuestionNumber(examId, request.getQuestionNumber())) {
            throw new DuplicateResourceException(
                    "Question number " + request.getQuestionNumber() + " already exists in this exam");
        }

        question.setQuestionNumber(request.getQuestionNumber());
        question.setContent(request.getContent());
        question.setQuestionType(request.getQuestionType());

        question.getAnswerOptions().clear();
        question.getAnswerOptions().addAll(buildOptions(request.getAnswerOption(), question));
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

    @Override
    public CheckAnswerResponse checkAnswer(Long examId, Long questionId, CheckAnswerRequest request) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!question.getExam().getId().equals(examId)) {
            throw new IllegalArgumentException("Question does not belong to this exam");
        }

        List<Long> correctOptionIds = question.getAnswerOptions().stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId)
                .toList();

        List<Long> selectedIds = request.getSelectedOptionIds() != null
                ? request.getSelectedOptionIds()
                : java.util.Collections.emptyList();

        boolean isUserCorrect = correctOptionIds.size() == selectedIds.size()
                && correctOptionIds.containsAll(selectedIds);

        return CheckAnswerResponse.builder()
                .isCorrect(isUserCorrect)
                .correctOptionIds(correctOptionIds)
                .build();
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
                .content(question.getContent())
                .questionNumber(question.getQuestionNumber())
                .imageUrl(question.getObjectKey() != null ? publicUrl + "/" + question.getObjectKey() : null)
                .questionType(question.getQuestionType())
                .answerOption(optionResponses)
                .build();
    }

    private QuestionResponse convertToUserQuestion(Question question) {
        List<AnswerOptionResponse> optionResponses = null;
        if (question.getAnswerOptions() != null && !question.getAnswerOptions().isEmpty()) {
            optionResponses = question.getAnswerOptions().stream().map(opt
                    -> AnswerOptionResponse.builder()
                            .id(opt.getId())
                            .optionLabel(opt.getOptionLabel())
                            .content(opt.getContent())
                            .isCorrect(false)
                            .build()
            ).toList();
        }
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .questionNumber(question.getQuestionNumber())
                .imageUrl(question.getObjectKey() != null ? publicUrl + "/" + question.getObjectKey() : null)
                .questionType(question.getQuestionType())
                .answerOption(optionResponses)
                .build();
    }
}
