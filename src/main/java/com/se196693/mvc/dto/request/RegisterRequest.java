package com.se196693.mvc.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 8, max = 50, message = "Username must be between 8 and 50 character")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 character")
    private String password;

    @NotBlank(message = "Email must be required")
    @Email(message = "Email must be valid")
    private String email;

}
