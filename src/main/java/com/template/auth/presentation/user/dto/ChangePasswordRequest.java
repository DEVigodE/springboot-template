package com.template.auth.presentation.user.dto;

import com.template.auth.presentation.shared.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank String currentPassword, @ValidPassword String newPassword) {
}
