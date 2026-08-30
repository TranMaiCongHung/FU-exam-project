package com.se196693.mvc.dto.response;

import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private Role role;
    private UserStatus status;
}
