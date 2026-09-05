package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.SubjectRequest;
import com.se196693.mvc.dto.response.SubjectResponse;
import com.se196693.mvc.entity.Subject;
import com.se196693.mvc.repository.SubjectRepository;
import com.se196693.mvc.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    public SubjectResponse createSubject(SubjectRequest subjectRequest) {
        Subject savedSubject = subjectRepository.save(convertToEntity(subjectRequest));
        return convertToResponse(savedSubject);
    }

    @Override
    public List<SubjectResponse> getSubjectsByTermNumber(Integer termNumber) {
        List<Subject> subjects = subjectRepository.findSubjectsByTermNumber(termNumber);
        return subjects.stream().map(this::convertToResponse).toList();
    }

    private SubjectResponse convertToResponse(Subject subject) {
        return SubjectResponse.builder()
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .termNumber(subject.getTermNumber())
                .build();
    }

    private Subject convertToEntity(SubjectRequest subjectRequest) {
        return Subject.builder()
                .subjectCode(subjectRequest.getSubjectCode())
                .subjectName(subjectRequest.getSubjectName())
                .termNumber(subjectRequest.getTermNumber())
                .build();
    }
}
