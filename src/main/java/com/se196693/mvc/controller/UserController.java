package com.se196693.mvc.controller;

import com.se196693.mvc.dto.request.UserCreationRequest;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")//SC, phân quyền trước khi method dc thực thi, cần khai báo @EnableMehthodSecutiry
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
           @Valid @RequestBody UserCreationRequest request){
        return ResponseEntity.ok(userService.createUser(request));
    }
}
