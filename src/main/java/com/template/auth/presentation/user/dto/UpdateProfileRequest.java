package com.template.auth.presentation.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Null fields are left unchanged (PATCH). */
public record UpdateProfileRequest(@Size(min = 2, max = 120) String name, @Email String email) {
}
