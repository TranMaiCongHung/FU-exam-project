package com.se196693.mvc.controller;

import com.se196693.mvc.dto.request.UserCreationRequest;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.service.UserService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")//SC, phân quyền trước khi method dc thực thi, cần khai báo @EnableMehthodSecutiry
    @PostMapping("/add")
    public ResponseEntity<UserResponse> createUser(
           @Valid @RequestBody UserCreationRequest request){
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(){
        return ResponseEntity.ok(userService.listUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id){
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> viewMyProfile(Authentication authentication){
        return ResponseEntity.ok(userService.viewMyProfile(authentication.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUser(@RequestParam(required = false) String keyword){
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }
}
