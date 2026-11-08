package com.template.auth.infrastructure.persistence.fileasset;

import com.template.auth.application.fileasset.FileAssetRepository;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.fileasset.FileAssetEntity;
import com.template.auth.infrastructure.persistence.shared.PageMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class FileAssetRepositoryAdapter implements FileAssetRepository {

    private final FileAssetJpaRepository jpa;

    public FileAssetRepositoryAdapter(FileAssetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FileAssetEntity create(FileAssetEntity fileAsset) {
        FileAssetJpaEntity entity = new FileAssetJpaEntity();
        entity.setId(fileAsset.getId());
        entity.setFileName(fileAsset.getFileName());
        entity.setMimeType(fileAsset.getMimeType());
        entity.setSize(fileAsset.getSize());
        entity.setStorageKey(fileAsset.getStorageKey());
        entity.setUploadedBy(fileAsset.getUploadedBy());
        entity.setCreatedAt(fileAsset.getCreatedAt());
        entity.setDeletedAt(fileAsset.getDeletedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileAssetEntity> findById(UUID id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(FileAssetRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<FileAssetEntity> findAllForUser(UUID userId, PaginationParams params) {
        var page = jpa.findAllByUploadedByAndDeletedAtIsNull(userId,
                PageMapper.toPageable(params, Sort.by("createdAt").descending()));
        return PageMapper.toResult(page, params, FileAssetRepositoryAdapter::toDomain);
    }

    @Override
    public void softDelete(UUID id) {
        jpa.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(Instant.now());
            jpa.save(entity);
        });
    }

    static FileAssetEntity toDomain(FileAssetJpaEntity entity) {
        return FileAssetEntity.reconstitute(entity.getId(), entity.getFileName(), entity.getMimeType(),
                entity.getSize(), entity.getStorageKey(), entity.getUploadedBy(), entity.getCreatedAt(),
                entity.getDeletedAt());
    }
}
