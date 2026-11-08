package com.template.auth.application.fileasset.command;

import com.template.auth.application.auditlog.AuditLogRecorder;
import com.template.auth.application.auditlog.AuditLogRecorder.RecordAuditLogInput;
import com.template.auth.application.fileasset.FileAssetRepository;
import com.template.auth.application.fileasset.FileStoragePort;
import com.template.auth.domain.fileasset.FileAssetEntity;
import com.template.auth.domain.shared.exception.FileAssetNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeleteFileHandler {

    private static final Logger log = LoggerFactory.getLogger(DeleteFileHandler.class);

    private final FileAssetRepository fileAssetRepository;
    private final FileStoragePort fileStorage;
    private final AuditLogRecorder auditLogRecorder;

    public DeleteFileHandler(FileAssetRepository fileAssetRepository, FileStoragePort fileStorage,
            AuditLogRecorder auditLogRecorder) {
        this.fileAssetRepository = fileAssetRepository;
        this.fileStorage = fileStorage;
        this.auditLogRecorder = auditLogRecorder;
    }

    public void execute(DeleteFileCommand command) {
        // Same exception for "doesn't exist" and "exists but isn't yours" -
        // deliberately not leaking existence of another user's file.
        FileAssetEntity fileAsset = fileAssetRepository.findById(command.fileAssetId())
                .filter(asset -> asset.isOwnedBy(command.actorId()))
                .orElseThrow(() -> new FileAssetNotFoundException(command.fileAssetId()));

        fileAssetRepository.softDelete(fileAsset.getId());

        // The DB row is already soft-deleted at this point, so the delete has
        // already succeeded from the caller's perspective - a failure to reclaim
        // the underlying bytes is logged, not thrown.
        try {
            fileStorage.delete(fileAsset.getStorageKey());
        } catch (IOException | RuntimeException e) {
            log.error("Failed to delete storage key \"{}\" for file asset {}", fileAsset.getStorageKey(),
                    fileAsset.getId(), e);
        }

        auditLogRecorder.recordAsync(new RecordAuditLogInput(command.actorId(), "file_asset.deleted", "FileAsset",
                fileAsset.getId().toString(), null));
    }
}
