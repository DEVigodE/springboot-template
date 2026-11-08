package com.template.auth.domain.fileasset;

import java.time.Instant;
import java.util.UUID;

/** Metadata of an uploaded file; the bytes themselves live behind FileStorage. */
public class FileAssetEntity {

    private final UUID id;
    private final String fileName;
    private final String mimeType;
    private final long size;
    private final String storageKey;
    private final UUID uploadedBy;
    private final Instant createdAt;
    private Instant deletedAt;

    private FileAssetEntity(UUID id, String fileName, String mimeType, long size, String storageKey,
            UUID uploadedBy, Instant createdAt, Instant deletedAt) {
        this.id = id;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static FileAssetEntity create(UUID id, String fileName, String mimeType, long size, String storageKey,
            UUID uploadedBy, Instant createdAt) {
        return new FileAssetEntity(id, fileName, mimeType, size, storageKey, uploadedBy, createdAt, null);
    }

    public static FileAssetEntity reconstitute(UUID id, String fileName, String mimeType, long size,
            String storageKey, UUID uploadedBy, Instant createdAt, Instant deletedAt) {
        return new FileAssetEntity(id, fileName, mimeType, size, storageKey, uploadedBy, createdAt, deletedAt);
    }

    public void markAsDeleted() {
        this.deletedAt = Instant.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return uploadedBy != null && uploadedBy.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
