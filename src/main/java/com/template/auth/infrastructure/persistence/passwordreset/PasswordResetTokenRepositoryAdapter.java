package com.template.auth.infrastructure.persistence.passwordreset;

import com.template.auth.application.passwordreset.PasswordResetTokenRepository;
import com.template.auth.domain.passwordreset.PasswordResetTokenEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;

    public PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PasswordResetTokenEntity create(PasswordResetTokenEntity token) {
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt());
        entity.setCreatedAt(token.getCreatedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(PasswordResetTokenRepositoryAdapter::toDomain);
    }

    @Override
    public void markAsUsed(UUID id) {
        jpa.findById(id).ifPresent(entity -> {
            entity.setUsedAt(Instant.now());
            jpa.save(entity);
        });
    }

    @Override
    public long deleteExpiredOrUsed() {
        return jpa.deleteExpiredOrUsed(Instant.now());
    }

    static PasswordResetTokenEntity toDomain(PasswordResetTokenJpaEntity entity) {
        return PasswordResetTokenEntity.reconstitute(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getUsedAt(), entity.getCreatedAt());
    }
}
