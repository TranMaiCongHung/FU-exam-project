package com.se196693.mvc.dto.request;

import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.Role;
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
public class UserProfileUpdateRequest implements  BaseUpdateUserRequest {
    private String fullName;

    @Override
    public void applyUpdateTo(User user) {
        if (this.fullName != null)
            user.setFullName(this.fullName);
    }
}
