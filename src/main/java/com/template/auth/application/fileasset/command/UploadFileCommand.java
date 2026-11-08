package com.template.auth.application.fileasset.command;

import java.util.UUID;

public record UploadFileCommand(String fileName, String mimeType, long size, byte[] content, UUID uploadedBy) {
}
