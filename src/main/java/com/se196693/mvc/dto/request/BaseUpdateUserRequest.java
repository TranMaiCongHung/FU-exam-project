package com.se196693.mvc.dto.request;

import com.se196693.mvc.entity.User;

public interface BaseUpdateUserRequest {
    void applyUpdateTo(User user);
}
