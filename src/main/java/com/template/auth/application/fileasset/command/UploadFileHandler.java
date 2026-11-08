package com.template.auth.application.fileasset.command;

import com.template.auth.application.fileasset.FileAssetDto;
import com.template.auth.application.fileasset.FileAssetRepository;
import com.template.auth.application.fileasset.FileStoragePort;
import com.template.auth.application.shared.FileTypeDetectorPort;
import com.template.auth.application.shared.NotificationQueuePort;
import com.template.auth.application.shared.NotificationQueuePort.NotificationJob;
import com.template.auth.domain.fileasset.FileAssetEntity;
import com.template.auth.domain.shared.exception.InvalidFileTypeException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UploadFileHandler {

    /**
     * Curated allowlist, not the client-supplied {@code command.mimeType()} -
     * that's just the request's Content-Type header, trivially spoofable. The
     * actual bytes are sniffed by FileTypeDetectorPort before anything is
     * written to storage. Extend this set as the template's real needs grow.
     */
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp");

    private static final DateTimeFormatter KEY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS");

    private final FileStoragePort fileStorage;
    private final FileAssetRepository fileAssetRepository;
    private final NotificationQueuePort notificationQueue;
    private final FileTypeDetectorPort fileTypeDetector;

    public UploadFileHandler(FileStoragePort fileStorage, FileAssetRepository fileAssetRepository,
            NotificationQueuePort notificationQueue, FileTypeDetectorPort fileTypeDetector) {
        this.fileStorage = fileStorage;
        this.fileAssetRepository = fileAssetRepository;
        this.notificationQueue = notificationQueue;
        this.fileTypeDetector = fileTypeDetector;
    }

    public FileAssetDto execute(UploadFileCommand command) {
        String detected = fileTypeDetector.detect(command.content()).orElse(null);

        if (detected == null || !ALLOWED_MIME_TYPES.contains(detected)) {
            throw new InvalidFileTypeException(detected);
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        // The file name's directory components are stripped (a crafted
        // "../../../../etc/cron.d/evil" never reaches the storage adapter) and the
        // asset id makes the key unique even for identical names.
        String storageKey = KEY_TIMESTAMP.format(now.atZone(java.time.ZoneOffset.UTC)) + "-" + id + "-"
                + safeBaseName(command.fileName());

        try {
            fileStorage.save(storageKey, command.content());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file " + storageKey, e);
        }

        FileAssetEntity saved = fileAssetRepository.create(FileAssetEntity.create(id, command.fileName(),
                detected, command.size(), storageKey, command.uploadedBy(), now));

        if (command.uploadedBy() != null) {
            notificationQueue.enqueue(new NotificationJob(command.uploadedBy(), "file.uploaded",
                    "Arquivo enviado", command.fileName() + " foi enviado com sucesso."));
        }

        return FileAssetDto.from(saved);
    }

    static String safeBaseName(String fileName) {
        String name = fileName == null ? "" : fileName.replace('\\', '/');
        String base = Paths.get(name).getFileName() == null ? "" : Paths.get(name).getFileName().toString();
        String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "file" : cleaned;
    }
}
