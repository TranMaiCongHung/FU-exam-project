package com.se196693.mvc.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
