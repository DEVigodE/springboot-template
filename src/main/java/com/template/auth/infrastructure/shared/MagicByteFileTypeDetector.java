package com.template.auth.infrastructure.shared;

import com.template.auth.application.shared.FileTypeDetectorPort;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Dependency-free magic-byte sniffer covering the types UploadFileHandler
 * allows. Swap for Apache Tika ({@code tika-core}) behind the same port if the
 * allowlist grows beyond a handful of signatures.
 */
@Component
public class MagicByteFileTypeDetector implements FileTypeDetectorPort {

    private static final byte[] PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87 = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF89 = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);

    @Override
    public Optional<String> detect(byte[] content) {
        if (content == null || content.length < 4) {
            return Optional.empty();
        }
        if (startsWith(content, 0, PDF)) {
            return Optional.of("application/pdf");
        }
        if (startsWith(content, 0, JPEG)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(content, 0, PNG)) {
            return Optional.of("image/png");
        }
        if (startsWith(content, 0, GIF87) || startsWith(content, 0, GIF89)) {
            return Optional.of("image/gif");
        }
        if (startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP)) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        return Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }
}
