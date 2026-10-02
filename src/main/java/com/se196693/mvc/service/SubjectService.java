package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.SubjectRequest;
import com.se196693.mvc.dto.response.SubjectResponse;
import com.se196693.mvc.entity.Subject;
import jakarta.persistence.criteria.CriteriaBuilder;

import java.util.List;

public interface SubjectService {
    SubjectResponse createSubject(SubjectRequest subjectRequest);

    List<SubjectResponse> getSubjectsByTermNumber(Integer termNumber);

    SubjectResponse updateSubject(Long id, SubjectRequest subjectRequest);

    List<SubjectResponse> getSubjects();

    void deleteSubject(Long id);

    Subject getSubjectById(Long id);
}
