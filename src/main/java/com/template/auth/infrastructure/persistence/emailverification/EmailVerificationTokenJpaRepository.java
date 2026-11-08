package com.template.auth.infrastructure.persistence.emailverification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenJpaEntity, UUID> {

    Optional<EmailVerificationTokenJpaEntity> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("delete from EmailVerificationTokenJpaEntity t where t.expiresAt < :now or t.usedAt is not null")
    int deleteExpiredOrUsed(@Param("now") Instant now);
}
