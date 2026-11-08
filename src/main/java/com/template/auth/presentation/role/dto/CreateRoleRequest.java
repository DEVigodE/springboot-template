package com.template.auth.presentation.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(@NotBlank @Size(min = 2, max = 60) String name, @Size(max = 255) String description) {
}
