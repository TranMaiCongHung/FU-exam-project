package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.SubjectRequest;
import com.se196693.mvc.dto.response.SubjectResponse;
import com.se196693.mvc.entity.Subject;
import com.se196693.mvc.exception.ResourceNotFoundException;
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

    @Override
    public SubjectResponse updateSubject(Long id, SubjectRequest subjectRequest) {
        Subject foundSubject = getSubjectById(id);

        foundSubject.setSubjectCode(subjectRequest.getSubjectCode());
        foundSubject.setSubjectName(subjectRequest.getSubjectName());
        foundSubject.setTermNumber(subjectRequest.getTermNumber());
        foundSubject.setStatus(subjectRequest.getStatus());

        subjectRepository.save(foundSubject);
        return convertToResponse(foundSubject);
    }

    @Override
    public List<SubjectResponse> getSubjects() {
        List<Subject> subjects = subjectRepository.findAll();
        return subjects.stream().map(this::convertToResponse).toList();
    }

    @Override
    public void deleteSubject(Long id) {
        Subject foundSubject = getSubjectById(id);
        subjectRepository.delete(foundSubject);
    }

    @Override
    public Subject getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Subject is not found")
        );
        return subject;
    }

    private SubjectResponse convertToResponse(Subject subject) {
        return SubjectResponse.builder()
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .termNumber(subject.getTermNumber())
                .status(subject.getStatus())
                .build();
    }

    private Subject convertToEntity(SubjectRequest subjectRequest) {
        return Subject.builder()
                .subjectCode(subjectRequest.getSubjectCode())
                .subjectName(subjectRequest.getSubjectName())
                .termNumber(subjectRequest.getTermNumber())
                .status(subjectRequest.getStatus())
                .build();
    }
}
