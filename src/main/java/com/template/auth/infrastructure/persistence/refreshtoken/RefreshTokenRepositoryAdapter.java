package com.template.auth.infrastructure.persistence.refreshtoken;

import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.domain.refreshtoken.RefreshTokenEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshTokenEntity create(RefreshTokenEntity token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevokedAt(token.getRevokedAt());
        entity.setCreatedAt(token.getCreatedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshTokenEntity> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public void revoke(UUID id) {
        jpa.findById(id).ifPresent(entity -> {
            entity.setRevokedAt(Instant.now());
            jpa.save(entity);
        });
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllForUser(userId, Instant.now());
    }

    @Override
    public long deleteExpiredOrRevoked() {
        return jpa.deleteExpiredOrRevoked(Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshTokenEntity> findActiveByUserId(UUID userId) {
        return jpa.findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(userId, Instant.now())
                .stream().map(RefreshTokenRepositoryAdapter::toDomain).toList();
    }

    static RefreshTokenEntity toDomain(RefreshTokenJpaEntity entity) {
        return RefreshTokenEntity.reconstitute(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getRevokedAt(), entity.getCreatedAt());
    }
}
