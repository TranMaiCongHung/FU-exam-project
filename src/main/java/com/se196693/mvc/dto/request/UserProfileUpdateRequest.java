package com.se196693.mvc.dto.request;

import com.se196693.mvc.entity.User;

public class UserProfileUpdateRequest implements  BaseUpdateUserRequest {
    private String username;
    private String fullName;
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
