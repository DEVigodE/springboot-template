package com.template.auth.application.fileasset.query;

import com.template.auth.application.fileasset.FileAssetRepository;
import com.template.auth.application.fileasset.FileStoragePort;
import com.template.auth.domain.fileasset.FileAssetEntity;
import com.template.auth.domain.shared.exception.FileAssetNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Service;

@Service
public class DownloadFileHandler {

    private final FileAssetRepository fileAssetRepository;
    private final FileStoragePort fileStorage;

    public DownloadFileHandler(FileAssetRepository fileAssetRepository, FileStoragePort fileStorage) {
        this.fileAssetRepository = fileAssetRepository;
        this.fileStorage = fileStorage;
    }

    /** The caller owns the stream and must close it (the controller hands it to the response). */
    public record DownloadedFile(String fileName, String mimeType, long size, InputStream stream) {
    }

    public DownloadedFile execute(DownloadFileQuery query) {
        // Same exception for "doesn't exist" and "exists but isn't yours" -
        // deliberately not leaking existence of another user's file.
        FileAssetEntity fileAsset = fileAssetRepository.findById(query.fileAssetId())
                .filter(asset -> asset.getDeletedAt() == null && asset.isOwnedBy(query.userId()))
                .orElseThrow(() -> new FileAssetNotFoundException(query.fileAssetId()));

        try {
            // Opening here (before any response header is written) surfaces a
            // missing file as a clean error response instead of a broken stream.
            InputStream stream = fileStorage.open(fileAsset.getStorageKey());
            return new DownloadedFile(fileAsset.getFileName(), fileAsset.getMimeType(), fileAsset.getSize(),
                    stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open storage key " + fileAsset.getStorageKey(), e);
        }
    }
}
