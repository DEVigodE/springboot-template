package com.template.auth.application.fileasset;

import com.template.auth.domain.fileasset.FileAssetEntity;
import java.time.Instant;
import java.util.UUID;

public record FileAssetDto(UUID id, String fileName, String mimeType, long size, UUID uploadedBy,
        Instant createdAt) {

    public static FileAssetDto from(FileAssetEntity fileAsset) {
        return new FileAssetDto(fileAsset.getId(), fileAsset.getFileName(), fileAsset.getMimeType(),
                fileAsset.getSize(), fileAsset.getUploadedBy(), fileAsset.getCreatedAt());
    }
}
