package com.template.auth.infrastructure.persistence.passwordreset;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("delete from PasswordResetTokenJpaEntity t where t.expiresAt < :now or t.usedAt is not null")
    int deleteExpiredOrUsed(@Param("now") Instant now);
}
