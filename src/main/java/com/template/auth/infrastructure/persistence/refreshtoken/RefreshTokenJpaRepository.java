package com.template.auth.infrastructure.persistence.refreshtoken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenJpaEntity> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(UUID userId,
            Instant now);

    @Transactional
    @Modifying
    @Query("update RefreshTokenJpaEntity t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("delete from RefreshTokenJpaEntity t where t.expiresAt < :now or t.revokedAt is not null")
    int deleteExpiredOrRevoked(@Param("now") Instant now);
}
