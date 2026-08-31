package com.se196693.mvc.dto.request;

import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest implements  BaseUpdateUserRequest {
    private String fullName;

    @Size(min = 8, max = 50, message = "Username must be between 8 and 50 character")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    @Override
    public void applyUpdateTo(User user) {
        if (this.username != null)
            user.setUsername(this.username);
        if (this.fullName != null)
            user.setFullName(this.fullName);
        if (this.email != null)
            user.setEmail(this.email);
    }
}
