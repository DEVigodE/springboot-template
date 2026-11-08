package com.template.auth.infrastructure.persistence.fileasset;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAssetJpaRepository extends JpaRepository<FileAssetJpaEntity, UUID> {

    Optional<FileAssetJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<FileAssetJpaEntity> findAllByUploadedByAndDeletedAtIsNull(UUID uploadedBy, Pageable pageable);
}
