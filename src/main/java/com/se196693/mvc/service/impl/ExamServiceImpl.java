package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.ExamFilterRequest;
import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.request.QuestionRequest;
import com.se196693.mvc.dto.response.ExamResponse;
import com.se196693.mvc.dto.response.PageResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.entity.Subject;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.ExamStatus;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.ExamRepository;
import com.se196693.mvc.repository.QuestionRepository;
import com.se196693.mvc.repository.SubjectRepository;
import com.se196693.mvc.repository.UserRepository;
import com.se196693.mvc.service.ExamService;
import com.se196693.mvc.service.FileStorageService;
import com.se196693.mvc.service.OcrService;
import com.se196693.mvc.service.QuestionService;
import com.se196693.mvc.specification.ExamSpecification;
import com.se196693.mvc.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final QuestionService questionService;

    @Override
    public ExamResponse createExam(Long subjectId, ExamRequest request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFoundException("User is not found")
        );

        Subject subject = subjectRepository.findById(subjectId).orElseThrow(
                () -> new ResourceNotFoundException("Subject is not found")
        );
        Exam savedExam = examRepository.save(
                Exam.builder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .user(user)
                        .semesterCode(request.getSemesterCode())
                        .examType(request.getExamType())
                        .subject(subject)
                        .build()
        );
        return convertToResponse(savedExam);
    }

    @Override
    public PageResponse<ExamResponse> getExams(ExamFilterRequest examFilterRequest, Pageable pageable) {
        ExamStatus effectiveStatus = isAdmin()
                ? examFilterRequest.getStatus()
                : ExamStatus.PUBLISHED;
        Specification<Exam> specification = Specification
                .where(ExamSpecification.hasSubject(examFilterRequest.getSubjectId()))
                .and(ExamSpecification.hasType(examFilterRequest.getExamType()))
                .and(ExamSpecification.hasStatus(effectiveStatus));

        Page<Exam> examPage = examRepository.findAll(specification, pageable);
        List<ExamResponse> content = examPage
                .getContent()
                .stream()
                .map(this::convertToResponse)
                .toList();
        return PageResponse.<ExamResponse>builder()
                .content(content)
                .currentPage(examPage.getNumber())
                .pageSize(examPage.getSize())
                .totalPages(examPage.getTotalPages())
                .totalElements(examPage.getTotalElements()).build();
    }

    @Override
    public List<QuestionRequest> extractOcr(Long examId, List<MultipartFile> documents) {
        Exam exam = examRepository.findById(examId).orElseThrow(
                () -> new ResourceNotFoundException("Exam not found")
        );

        List<QuestionRequest> allExtractedQuestions = new java.util.ArrayList<>();

        // Xử lý từng file ảnh (đảm bảo câu hỏi thuộc ảnh nào sẽ gắn liền với ảnh đó)
        for (MultipartFile doc : documents) {
            String extension = FileUtils.getExtension(doc.getOriginalFilename());
            String originalObjectKey = "exams/" + examId + "/original-" + java.util.UUID.randomUUID() + "." + extension;

            // 1. Upload lên R2/S3 và lấy URL ảnh
            String imageUrl = fileStorageService.upload(doc, originalObjectKey);

            // 2. Gọi Gemini OCR bóc tách câu hỏi từ bức ảnh này
            List<QuestionRequest> questionsFromDoc = ocrService.extractQuestionsFromImage(List.of(doc));

            // 3. Gán chính xác objectKey và imageUrl vào từng câu hỏi
            if (questionsFromDoc != null) {
                for (QuestionRequest q : questionsFromDoc) {
                    q.setObjectKey(originalObjectKey);
                    q.setImageUrl(imageUrl);
                }
                allExtractedQuestions.addAll(questionsFromDoc);
            }
        }

        // Đổi trạng thái bài thi sang REVIEW
        exam.setStatus(com.se196693.mvc.enums.ExamStatus.REVIEW);
        examRepository.save(exam);

        return allExtractedQuestions;
    }

    @Override
    public ExamResponse updateVisibility(Long examId, Boolean visible) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        if (visible != null) {
            exam.setStatus(visible ? ExamStatus.PUBLISHED : ExamStatus.HIDDEN);
        } else {
            // Không truyền visible thì tự đảo trạng thái
            exam.setStatus(exam.getStatus() == ExamStatus.PUBLISHED
                    ? ExamStatus.HIDDEN
                    : ExamStatus.PUBLISHED);
        }

        Exam saved = examRepository.save(exam);
        return convertToResponse(saved);
    }

    @Override
    public ExamResponse updateAllowViewImage(Long examId, Boolean allowed) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        if (allowed != null) {
            exam.setAllowViewImage(allowed);
        } else {
            // Không truyền allowed thì tự đảo trạng thái (null coi như true)
            exam.setAllowViewImage(Boolean.FALSE.equals(exam.getAllowViewImage()));
        }

        Exam saved = examRepository.save(exam);
        return convertToResponse(saved);
    }

    private ExamResponse convertToResponse(Exam exam){
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .semesterCode(exam.getSemesterCode())
                .examType(exam.getExamType())
                .createdAt(exam.getCreatedAt())
                .status(exam.getStatus())
                .allowViewImage(!Boolean.FALSE.equals(exam.getAllowViewImage()))
                .build();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
