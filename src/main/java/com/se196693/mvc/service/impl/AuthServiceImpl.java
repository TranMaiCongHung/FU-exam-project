package com.se196693.mvc.service.impl;

import com.se196693.mvc.dto.request.LoginRequest;
import com.se196693.mvc.dto.request.RefreshTokenRequest;
import com.se196693.mvc.dto.response.LoginResponse;
import com.se196693.mvc.entity.RefreshToken;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.UserStatus;
import com.se196693.mvc.exception.AccountStatusException;
import com.se196693.mvc.exception.InvalidCredentialsException;
import com.se196693.mvc.exception.ResourceNotFoundException;
import com.se196693.mvc.repository.RefreshTokenRepository;
import com.se196693.mvc.security.JwtService;
import com.se196693.mvc.service.AuthService;
import com.se196693.mvc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration:604800000}") // Mặc định 7 ngày
    private Long refreshTokenDurationMs;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user;
        try {
            user = userService.findByUsername(request.getUsername());
        } catch (ResourceNotFoundException e) {
            throw new InvalidCredentialsException("Invalid username or password", e);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccountStatusException("Account is inactive.");
        }

        if (user.getStatus().equals(UserStatus.BLOCKED)) {
            throw new AccountStatusException("Account is blocked");
        }

        // Tạo access token mới
        String accessToken = jwtService.generateToken(user);
// Tìm xem user đã có refresh token chưa. Nếu có thì lấy ra, chưa có thì tạo mới
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());
// Update lại các thông tin của token
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
// Lưu vào DB (Hibernate sẽ tự hiểu là Update nếu đã tồn tại, hoặc Insert nếu tạo mới)
        refreshTokenRepository.save(refreshToken);
        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                user.getUsername(),
                user.getRole(),
                user.getStatus()
        );
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        // Tìm token trong DB
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token is not in database!"));
        // Kiểm tra hạn sử dụng
        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidCredentialsException("Refresh token was expired. Please make a new signin request");
        }
        User user = refreshToken.getUser();

        // Tạo access token mới
        String newAccessToken = jwtService.generateToken(user);
        return new LoginResponse(
                newAccessToken,
                requestRefreshToken, // Giữ nguyên refresh token cũ
                user.getUsername(),
                user.getRole(),
                user.getStatus()
        );
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }
}
