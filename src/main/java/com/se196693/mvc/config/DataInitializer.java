package com.se196693.mvc.config;

import com.se196693.mvc.enums.AuthProvider;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import com.se196693.mvc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();

                admin.setFullName("Admin Tran");
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setEmail("admin@gmail.com");
                admin.setRole(Role.ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setAuthProvider(AuthProvider.LOCAL);
                admin.setProviderId(null);

                userRepository.save(admin);
            }

            if (userRepository.findByUsername("user123").isEmpty()) {
                User user = new User();

                user.setFullName("Tran Hung");
                user.setUsername("user123");
                user.setPassword(passwordEncoder.encode("User@123"));
                user.setEmail("user@gmail.com");
                user.setRole(Role.USER);
                user.setStatus(UserStatus.ACTIVE);
                user.setAuthProvider(AuthProvider.LOCAL);
                user.setProviderId(null);
                userRepository.save(user);
            }
        };
    }
}