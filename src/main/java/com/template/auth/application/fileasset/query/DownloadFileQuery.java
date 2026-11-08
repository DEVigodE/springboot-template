package com.template.auth.application.fileasset.query;

import java.util.UUID;

public record DownloadFileQuery(UUID fileAssetId, UUID userId) {
}
