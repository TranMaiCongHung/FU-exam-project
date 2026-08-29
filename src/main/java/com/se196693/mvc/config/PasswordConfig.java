package com.se196693.mvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration// đánh dấu class là một cấu hình, cho phép Spring quản lý các bean được định nghĩa trong class này
public class PasswordConfig {

    @Bean //lưu các đối tượng được khai báo là Bean vào trong Spring container
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //tạo ra một object BCryptPasswordEncoder, 
    }
}
