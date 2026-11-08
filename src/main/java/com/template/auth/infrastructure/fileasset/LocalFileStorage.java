package com.template.auth.infrastructure.fileasset;

import com.template.auth.application.fileasset.FileStoragePort;
import com.template.auth.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default FileStoragePort adapter: bytes on local disk. Fine for a
 * single-instance/dev deployment; set FILE_STORAGE=s3 for S3FileStorage in
 * multi-instance/production use.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStoragePort {

    private final Path uploadDir;

    @Autowired
    public LocalFileStorage(AppProperties properties) {
        this(Path.of(properties.storage().localDir()));
    }

    LocalFileStorage(Path uploadDir) {
        this.uploadDir = uploadDir.toAbsolutePath().normalize();
    }

    @Override
    public void save(String storageKey, byte[] content) throws IOException {
        Files.createDirectories(uploadDir);
        Files.write(resolveWithinUploadDir(storageKey), content);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        Path path = resolveWithinUploadDir(storageKey);
        // Surfaces a missing file as a clean failure here, before any response
        // headers are sent - a stream error mid-transfer can't be turned into a
        // normal HTTP error response anymore.
        if (!Files.isRegularFile(path)) {
            throw new NoSuchFileException(path.toString());
        }
        return Files.newInputStream(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveWithinUploadDir(storageKey));
    }

    /**
     * Confines the resolved path to the upload directory regardless of what the
     * key contains. The key is derived from a client-supplied file name upstream
     * (see UploadFileHandler) - this is the adapter's own last line of defense
     * against a "../../etc/x"-style key, independent of upstream sanitizing.
     */
    Path resolveWithinUploadDir(String storageKey) {
        Path resolved = uploadDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(uploadDir) || resolved.equals(uploadDir)) {
            throw new IllegalArgumentException(
                    "Invalid storage key \"" + storageKey + "\": resolves outside the upload directory");
        }
        return resolved;
    }
}
