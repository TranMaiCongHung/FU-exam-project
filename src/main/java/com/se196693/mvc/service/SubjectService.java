package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.SubjectRequest;
import com.se196693.mvc.dto.response.SubjectResponse;

import java.util.List;

public interface SubjectService {
    SubjectResponse createSubject(SubjectRequest subjectRequest);

    List<SubjectResponse> getSubjectsByTermNumber(Integer termNumber);
}
