package com.template.auth.infrastructure.persistence.permission;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, UUID> {

    Optional<PermissionJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PermissionJpaEntity> findByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    Page<PermissionJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
