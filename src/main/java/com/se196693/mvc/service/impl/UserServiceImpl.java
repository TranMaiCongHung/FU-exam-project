package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.RegisterRequest;
import com.se196693.mvc.dto.request.UserCreationRequest;
import com.se196693.mvc.dto.request.UserRequest;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import com.se196693.mvc.exception.DuplicateResourceException;
import com.se196693.mvc.exception.InvalidCredentialsException;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.UserRepository;
import com.se196693.mvc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public <T extends UserRequest> UserResponse createUser(T register) {
        User user = convertToEntity(register);
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    @Override
    public List<UserResponse> listUsers() {
        List<User> list = userRepository.findAll();
        List<UserResponse> responseList = list.stream().map(this::convertToResponse).toList();
        return responseList;
    }

    @Override
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        return convertToResponse(user);
    }

    @Override
    public UserResponse viewMyProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        return convertToResponse(user);
    }

    @Override
    public List<UserResponse> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isBlank()){
            return listUsers();
        }
        List<User> foundUser = userRepository.findUserByUsernameOrFullNameContainingIgnoreCase(keyword.trim(),keyword.trim());
        if (foundUser.isEmpty()){
            throw new ResourceNotFoundException("No users found");
        }
        return foundUser.stream().map(this::convertToResponse).toList();
    }

    public UserResponse convertToResponse(User user){
        return new UserResponse(user.getId(), user.getFullName(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus());
    }

    public <T extends UserRequest> User convertToEntity(T request) {
        if (request instanceof RegisterRequest reg) {
            User user = new User();
            user.setFullName(reg.getFullName());
            user.setUsername(reg.getUsername());
            user.setPassword(passwordEncoder.encode(reg.getPassword()));
            user.setEmail(reg.getEmail());
            user.setRole(Role.USER);
            user.setStatus(UserStatus.ACTIVE);
            return user;
        }
        if (request instanceof UserCreationRequest reg) {
            User user = new User();
            user.setFullName(reg.getFullName());
            user.setUsername(reg.getUsername());
            user.setPassword(passwordEncoder.encode(reg.getPassword()));
            user.setEmail(reg.getEmail());
            user.setRole(reg.getRole());
            user.setStatus(UserStatus.INACTIVE);
            return user;
        }
        throw new IllegalArgumentException("Unsupported request DTO: " + request.getClass().getName());
    }
}
