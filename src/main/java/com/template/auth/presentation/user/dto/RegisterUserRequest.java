package com.template.auth.presentation.user.dto;

import com.template.auth.presentation.shared.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Email String email,
        @ValidPassword String password) {
}
