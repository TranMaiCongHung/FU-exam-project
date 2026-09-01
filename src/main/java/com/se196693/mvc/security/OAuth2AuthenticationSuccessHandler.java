package com.se196693.mvc.security;

import com.se196693.mvc.entity.User;
import com.se196693.mvc.exception.DuplicateResourceException;
import com.se196693.mvc.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// lấy data từ gg, gọi service luu db, tạo jwt token trả về FE
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //lấy infor gg trả về
        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        try {
            User user = userService.processGoogleUser(oauth2User);
            String token = jwtService.generateToken(user);

            response.sendRedirect("http://localhost:3000/oauth2/success?token=" + token);

        } catch (DuplicateResourceException e) {
            response.sendRedirect("http://localhost:3000/oauth2/error?message=email_is_used");
        }
    }
}
