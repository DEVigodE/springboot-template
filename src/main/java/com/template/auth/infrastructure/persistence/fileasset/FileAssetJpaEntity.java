package com.template.auth.infrastructure.persistence.fileasset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_assets")
@Getter
@Setter
@NoArgsConstructor
public class FileAssetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(nullable = false)
    private long size;

    @Column(name = "storage_key", nullable = false, length = 320)
    private String storageKey;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
