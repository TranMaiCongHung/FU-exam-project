package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.*;
import com.se196693.mvc.dto.response.PageResponse;
import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.AuthProvider;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import com.se196693.mvc.exception.DuplicateResourceException;
import com.se196693.mvc.exception.InvalidCredentialsException;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.UserRepository;
import com.se196693.mvc.service.UserService;
import com.se196693.mvc.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    @Value("${app.admin.emails:}")
    private List<String> adminEmails;

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
    public PageResponse<UserResponse> listUsers(UserFilterRequest filter, Pageable pageable) {
        Specification<User> specification = Specification
                .where(UserSpecification.hasKeyword(filter.getKeyword()))
                .and(UserSpecification.hasRole(filter.getRole()))
                .and(UserSpecification.hasStatus(filter.getStatus()));

        Page<User> userPage = userRepository.findAll(specification, pageable);

        List<UserResponse> content = userPage
                .getContent()
                .stream()
                .map(this::convertToResponse)
                .toList();
        return PageResponse.<UserResponse>builder()
                .content(content)
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements()).build();
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
    public <T extends BaseUpdateUserRequest> UserResponse updateUser(Long id, T request) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        request.applyUpdateTo(user);
        userRepository.save(user);
        return convertToResponse(user);
    }

    @Override
    public <T extends BaseUpdateUserRequest> UserResponse updateUser(String username, T request) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        request.applyUpdateTo(user);
        userRepository.save(user);
        return convertToResponse(user);
    }

    @Override
    public User processGoogleUser(OAuth2User oauth2User) {
        String googleId
                = oauth2User.getAttribute("sub");

        String email
                = oauth2User.getAttribute("email");

        String fullName
                = oauth2User.getAttribute("name");

        // 1. Tìm Google account
        Optional<User> existingUser
                = userRepository.findByAuthProviderAndProviderId(
                        AuthProvider.GOOGLE,
                        googleId
                );

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // 2. Kiểm tra email đã tồn tại chưa
        Optional<User> userByEmail
                = userRepository.findByEmail(email);

        if (userByEmail.isPresent()) {
            throw new DuplicateResourceException(
                    "Email is already registered with another authentication method"
            );
        }

        Role userRole = adminEmails.contains(email) ? Role.ADMIN : Role.USER;
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .username(generateGoogleUsername(email))
                .role(userRole)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(googleId)
                .build();

        return userRepository.save(user);
    }

    public UserResponse convertToResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus());
    }

    public <T extends UserRequest> User convertToEntity(T request) {
        if (request instanceof RegisterRequest reg) {
            return User.builder()
                    .fullName(reg.getFullName())
                    .username(reg.getUsername())
                    .password(passwordEncoder.encode(reg.getPassword()))
                    .email(reg.getEmail())
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .authProvider(AuthProvider.LOCAL)
                    .build();
        }
        if (request instanceof UserCreationRequest reg) {
            return User.builder()
                    .fullName(reg.getFullName())
                    .username(reg.getUsername())
                    .password(passwordEncoder.encode(reg.getPassword()))
                    .email(reg.getEmail())
                    .role(reg.getRole())
                    .status(UserStatus.INACTIVE)
                    .authProvider(AuthProvider.LOCAL)
                    .build();
        }
        throw new IllegalArgumentException("Unsupported request DTO: " + request.getClass().getName());
    }

    private String generateGoogleUsername(String email) {

        String baseUsername
                = email.substring(0, email.indexOf("@"));

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
