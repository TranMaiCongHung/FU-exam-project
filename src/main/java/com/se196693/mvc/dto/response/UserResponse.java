package com.se196693.mvc.dto.response;

import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private Role role;
    private UserStatus status;
}
