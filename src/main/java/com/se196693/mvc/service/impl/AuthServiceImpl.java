package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.LoginRequest;
import com.se196693.mvc.dto.response.LoginResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.security.JwtService;
import com.se196693.mvc.service.AuthService;
import com.se196693.mvc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole()
        );
    }
}
