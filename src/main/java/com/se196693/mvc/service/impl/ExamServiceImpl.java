package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.ExamFilterRequest;
import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.response.ExamResponse;
import com.se196693.mvc.dto.response.PageResponse;
import com.se196693.mvc.dto.response.QuestionResponse;
import com.se196693.mvc.entity.Exam;
import com.se196693.mvc.entity.Subject;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.ExamRepository;
import com.se196693.mvc.repository.QuestionRepository;
import com.se196693.mvc.repository.SubjectRepository;
import com.se196693.mvc.repository.UserRepository;
import com.se196693.mvc.service.ExamService;
import com.se196693.mvc.specification.ExamSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        Specification<Exam> specification = Specification
                .where(ExamSpecification.hasSubject(examFilterRequest.getSubjectId()))
                .and(ExamSpecification.hasType(examFilterRequest.getExamType()));

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

    private ExamResponse convertToResponse(Exam exam){
        return ExamResponse.builder()
                .title(exam.getTitle())
                .description(exam.getDescription())
                .semesterCode(exam.getSemesterCode())
                .examType(exam.getExamType())
                .build();
    }
}
