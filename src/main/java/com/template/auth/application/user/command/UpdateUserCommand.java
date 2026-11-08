package com.template.auth.application.user.command;

import java.util.UUID;

/** Null fields are "leave unchanged" (PATCH semantics). */
public record UpdateUserCommand(UUID userId, String name, String email, Boolean active) {
}
