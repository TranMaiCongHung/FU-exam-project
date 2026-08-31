package com.se196693.mvc.dto.request;

import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateUserRequest implements  BaseUpdateUserRequest {
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;

    @Override
    public void applyUpdateTo(User user) {
        if (this.fullName != null)
            user.setFullName(this.fullName);
        if (this.username != null)
            user.setUsername(this.username);
        if (this.email != null)
            user.setEmail(this.email);
        if (this.role != null)
            user.setRole(this.role);
        if (this.status != null)
            user.setStatus(this.status);
    }
}
