package com.template.auth.application.refreshtoken.query;

import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListSessionsHandler {

    private final RefreshTokenRepository refreshTokenRepository;

    public ListSessionsHandler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public record SessionDto(UUID id, Instant createdAt, Instant expiresAt) {
    }

    public List<SessionDto> execute(ListSessionsQuery query) {
        return refreshTokenRepository.findActiveByUserId(query.userId()).stream()
                .map(session -> new SessionDto(session.getId(), session.getCreatedAt(), session.getExpiresAt()))
                .toList();
    }
}
