package com.template.auth.infrastructure.fileasset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripsBytesInsideTheUploadDirectory() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(tempDir.resolve("uploads"));

        storage.save("a.txt", "hello".getBytes());

        assertThat(Files.exists(tempDir.resolve("uploads").resolve("a.txt"))).isTrue();
        try (InputStream in = storage.open("a.txt")) {
            assertThat(new String(in.readAllBytes())).isEqualTo("hello");
        }

        storage.delete("a.txt");
        assertThat(Files.exists(tempDir.resolve("uploads").resolve("a.txt"))).isFalse();
    }

    @Test
    void refusesKeysThatEscapeTheUploadDirectory() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.resolve("uploads"));

        assertThatThrownBy(() -> storage.save("../escape.txt", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.open("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsCleanlyWhenOpeningAMissingFile() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.resolve("uploads"));

        assertThatThrownBy(() -> storage.open("missing.bin")).isInstanceOf(NoSuchFileException.class);
    }
}
