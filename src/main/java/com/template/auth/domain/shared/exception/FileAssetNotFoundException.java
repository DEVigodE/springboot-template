package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class FileAssetNotFoundException extends DomainException {

    public FileAssetNotFoundException(Object fileAssetId) {
        super("File \"" + fileAssetId + "\" not found");
    }
}
