package com.template.auth.application.refreshtoken.command;

import java.util.UUID;

public record RevokeSessionCommand(UUID sessionId, UUID userId) {
}
