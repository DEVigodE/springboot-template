package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidFileTypeException extends DomainException {

    public InvalidFileTypeException(String detectedMimeType) {
        super(detectedMimeType != null
                ? "File type \"" + detectedMimeType + "\" is not allowed"
                : "File type could not be determined");
    }
}
