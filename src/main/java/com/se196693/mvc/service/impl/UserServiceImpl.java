package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.RegisterRequest;
import com.se196693.mvc.dto.request.UserCreationRequest;
import com.se196693.mvc.dto.request.UserRequest;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.repository.UserRepository;
import com.se196693.mvc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public <T extends UserRequest> UserResponse createUser(T register) {
        User user = convertToEntity(register);
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public UserResponse convertToResponse(User user){
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public <T extends UserRequest> User convertToEntity(T request) {
        if (request instanceof RegisterRequest reg) {
            User user = new User();
            user.setUsername(reg.getUsername());
            user.setPassword(passwordEncoder.encode(reg.getPassword()));
            user.setEmail(reg.getEmail());
            user.setRole(Role.USER);
            return user;
        }
        if (request instanceof UserCreationRequest reg) {
            User user = new User();
            user.setUsername(reg.getUsername());
            user.setPassword(passwordEncoder.encode(reg.getPassword()));
            user.setEmail(reg.getEmail());
            user.setRole(reg.getRole());
            return user;
        }
        throw new IllegalArgumentException("Unsupported request DTO: " + request.getClass().getName());
    }
}
