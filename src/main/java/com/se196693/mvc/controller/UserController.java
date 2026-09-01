package com.se196693.mvc.controller;

import java.util.List;

import com.se196693.mvc.dto.request.AdminUpdateUserRequest;
import com.se196693.mvc.dto.request.UserFilterRequest;
import com.se196693.mvc.dto.request.UserProfileUpdateRequest;
import com.se196693.mvc.dto.response.PageResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.se196693.mvc.dto.request.UserCreationRequest;
import com.se196693.mvc.dto.response.ApiResponse;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.enums.UserStatus;
import com.se196693.mvc.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")//SC, phân quyền trước khi method dc thực thi, cần khai báo @EnableMehthodSecutiry
    @PostMapping("/admin/users/add")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "User registered successfully",
                        userService.createUser(request))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            //@ParameterObject: chia object thanh tung truong nho hien thi tren swagger
            @ParameterObject @ModelAttribute UserFilterRequest filter,
            @ParameterObject Pageable pageable
            ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Listed users successfully",
                        userService.listUsers(filter,pageable))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Fetched user successfully",
                        userService.getUser(id))
        );

    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> viewMyProfile(Authentication authentication) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Fetched profile successfully",
                        userService.viewMyProfile(authentication.getName()))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateInfoByAdmin(
            @PathVariable Long id,
            @Valid
            @RequestBody  AdminUpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Updated status successfully",
                        userService.updateUser(id, request)
                )
        );
    }

     @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Authentication authentication,
            @Valid
            @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Updated profile successfully",
                        // Gọi hàm updateUser overload mới tạo ở trên
                        userService.updateUser(authentication.getName(), request)
                )
        );
    }


}
