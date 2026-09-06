package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.ExamFilterRequest;
import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.response.ExamResponse;
import com.se196693.mvc.dto.response.PageResponse;
import com.se196693.mvc.entity.User;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface ExamService {
    ExamResponse createExam(Long subjectId,ExamRequest request, String username);

    PageResponse<ExamResponse> getExams(ExamFilterRequest examFilterRequest, Pageable pageable);
}
