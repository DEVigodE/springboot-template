package com.template.auth.application.fileasset;

import java.io.IOException;
import java.io.InputStream;

/**
 * Port for the actual file bytes, kept separate from FileAssetRepository (which
 * only persists metadata). Swap the bound adapter (FILE_STORAGE=local|s3)
 * without touching schema, handlers, or the API.
 */
public interface FileStoragePort {

    void save(String storageKey, byte[] content) throws IOException;

    /** Streamed, not buffered - a download shouldn't hold the whole file in memory. */
    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
