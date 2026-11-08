package com.template.auth.application.fileasset;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.fileasset.FileAssetEntity;
import java.util.Optional;
import java.util.UUID;

/** Metadata only - the bytes live behind FileStoragePort. */
public interface FileAssetRepository {

    FileAssetEntity create(FileAssetEntity fileAsset);

    /** Non-deleted assets only. */
    Optional<FileAssetEntity> findById(UUID id);

    PaginatedResult<FileAssetEntity> findAllForUser(UUID userId, PaginationParams params);

    void softDelete(UUID id);
}
