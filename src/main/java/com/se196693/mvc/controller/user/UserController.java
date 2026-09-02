package com.se196693.mvc.controller.user;

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
import com.se196693.mvc.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> viewMyProfile(Authentication authentication) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Fetched profile successfully",
                        userService.viewMyProfile(authentication.getName()))
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
