package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.response.ExamResponse;
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
import lombok.RequiredArgsConstructor;
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
    public List<ExamResponse> getExams(Long subjectId) {
        List<Exam> exams = examRepository.findByS
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
