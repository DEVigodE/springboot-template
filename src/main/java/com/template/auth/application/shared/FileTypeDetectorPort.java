package com.template.auth.application.shared;

import java.util.Optional;

/** Content-based (magic bytes) MIME detection - never trusts the client's Content-Type. */
public interface FileTypeDetectorPort {

    Optional<String> detect(byte[] content);
}
