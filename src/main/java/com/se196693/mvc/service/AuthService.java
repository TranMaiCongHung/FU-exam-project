package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.LoginRequest;
import com.se196693.mvc.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
