package com.se196693.mvc.dto.request;

import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import com.se196693.mvc.enums.UserStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateUserRequest implements  BaseUpdateUserRequest {

    private String fullName;

    @Size(min = 8, max = 50, message = "Username must be between 8 and 50 character")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    private Role role;

    @Enumerated(EnumType.STRING)
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
