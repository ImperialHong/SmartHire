package com.smarthire.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email,

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 64, message = "password length must be between 8 and 64")
    String password,

    @NotBlank(message = "fullName is required")
    @Size(max = 100, message = "fullName length must be less than or equal to 100")
    String fullName,

    @Size(max = 20, message = "phone length must be less than or equal to 20")
    String phone,

    @NotBlank(message = "roleCode is required")
    String roleCode
) {
}
