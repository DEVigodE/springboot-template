package com.template.auth.application.user.command;

import java.util.UUID;

/** {@code actorId} is who performed the deletion (self or an admin); null when unknown. */
public record DeleteUserCommand(UUID userId, UUID actorId) {
}
