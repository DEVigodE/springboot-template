package com.template.auth.application.fileasset.command;

import java.util.UUID;

public record DeleteFileCommand(UUID fileAssetId, UUID actorId) {
}
