package com.se196693.mvc.security;

import com.se196693.mvc.entity.User;

public interface JwtService {
    String generateToken(User user);

    String extractUsername(String token);

    boolean isTokenValid(String token, User user);
}
