package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.RegisterRequest;
import com.se196693.mvc.dto.request.UserRequest;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;

import java.util.Optional;

public interface UserService {
    User findByUsername(String username);

    <T extends UserRequest> UserResponse createUser(T register);

}
