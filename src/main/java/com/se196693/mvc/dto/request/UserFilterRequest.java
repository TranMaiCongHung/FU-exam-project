package com.se196693.mvc.dto.request;

import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFilterRequest {
    private String keyword;

    private Role role;

    private UserStatus status;
}
