package com.se196693.mvc.controller;

import com.se196693.mvc.dto.request.LoginRequest;
import com.se196693.mvc.dto.request.RegisterRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.LoginResponse;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.service.AuthService;
import com.se196693.mvc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successfully",
                        authService.login(request))
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "User registered successfully",
                        userService.createUser(request))
        );
    }
}
