package com.template.auth.infrastructure.persistence.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserJpaEntity> findWithRolesByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserJpaEntity> findWithRolesByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, UUID id);

    // Two-step paging: ids first (SQL LIMIT/OFFSET), then the graph for just
    // that page - a fetch join + LIMIT would otherwise be paginated in memory.
    @Query("select u.id from UserJpaEntity u where u.deletedAt is null")
    Page<UUID> findIdsByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    List<UserJpaEntity> findWithRolesByIdIn(Collection<UUID> ids);
}
