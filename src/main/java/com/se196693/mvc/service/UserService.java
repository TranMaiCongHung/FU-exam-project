package com.se196693.mvc.service;

import com.se196693.mvc.dto.request.AdminUpdateUserRequest;
import com.se196693.mvc.dto.request.BaseUpdateUserRequest;
import com.se196693.mvc.dto.request.RegisterRequest;
import com.se196693.mvc.dto.request.UserRequest;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.UserStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {
    User findByUsername(String username);

    <T extends UserRequest> UserResponse createUser(T register);

    List<UserResponse> listUsers();

    UserResponse getUser(Long id);

    UserResponse viewMyProfile(String username);

    List<UserResponse> searchUsers(String keyword);

    <T extends BaseUpdateUserRequest> UserResponse updateUser(Long id,T request);
    <T extends BaseUpdateUserRequest> UserResponse updateUser(String username, T request);
}
