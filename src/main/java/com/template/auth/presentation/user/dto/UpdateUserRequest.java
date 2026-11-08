package com.template.auth.presentation.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Admin variant: can also (de)activate the account. Null fields are left unchanged. */
public record UpdateUserRequest(@Size(min = 2, max = 120) String name, @Email String email, Boolean active) {
}
