package com.template.auth.infrastructure.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MagicByteFileTypeDetectorTest {

    private final MagicByteFileTypeDetector detector = new MagicByteFileTypeDetector();

    @Test
    void detectsTheAllowedTypesFromTheirSignatures() {
        assertThat(detector.detect("%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII))).contains("application/pdf");
        assertThat(detector.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}))
                .contains("image/jpeg");
        assertThat(detector.detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}))
                .contains("image/png");
        assertThat(detector.detect("GIF89a....".getBytes(StandardCharsets.US_ASCII))).contains("image/gif");
        assertThat(detector.detect("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)))
                .contains("image/webp");
    }

    @Test
    void returnsEmptyForUnknownOrTooShortContent() {
        assertThat(detector.detect("hello world".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(detector.detect("RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(detector.detect(new byte[] {1, 2})).isEmpty();
        assertThat(detector.detect(null)).isEmpty();
    }
}
