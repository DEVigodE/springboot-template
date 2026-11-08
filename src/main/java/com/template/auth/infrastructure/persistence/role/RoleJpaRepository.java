package com.template.auth.infrastructure.persistence.role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleJpaEntity> findWithPermissionsByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleJpaEntity> findWithPermissionsByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    // Two-step paging: ids first (SQL LIMIT/OFFSET), then the graph for just
    // that page - a fetch join + LIMIT would otherwise be paginated in memory.
    @Query("select r.id from RoleJpaEntity r where r.deletedAt is null")
    Page<UUID> findIdsByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "permissions")
    List<RoleJpaEntity> findWithPermissionsByIdIn(Collection<UUID> ids);
}
