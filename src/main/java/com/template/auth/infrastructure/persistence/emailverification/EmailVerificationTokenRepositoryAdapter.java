package com.template.auth.infrastructure.persistence.emailverification;

import com.template.auth.application.emailverification.EmailVerificationTokenRepository;
import com.template.auth.domain.emailverification.EmailVerificationTokenEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpa;

    public EmailVerificationTokenRepositoryAdapter(EmailVerificationTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public EmailVerificationTokenEntity create(EmailVerificationTokenEntity token) {
        EmailVerificationTokenJpaEntity entity = new EmailVerificationTokenJpaEntity();
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
    public Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(EmailVerificationTokenRepositoryAdapter::toDomain);
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

    static EmailVerificationTokenEntity toDomain(EmailVerificationTokenJpaEntity entity) {
        return EmailVerificationTokenEntity.reconstitute(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getUsedAt(), entity.getCreatedAt());
    }
}
