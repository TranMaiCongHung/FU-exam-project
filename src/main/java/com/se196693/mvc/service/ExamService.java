package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.ExamRequest;
import com.se196693.mvc.dto.response.ExamResponse;
import com.se196693.mvc.entity.User;


import java.util.List;

public interface ExamService {
    ExamResponse createExam(ExamRequest request, String username);


}
