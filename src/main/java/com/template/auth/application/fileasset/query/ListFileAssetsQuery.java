package com.template.auth.application.fileasset.query;

import com.template.auth.application.shared.PaginationParams;
import java.util.UUID;

public record ListFileAssetsQuery(UUID userId, PaginationParams pagination) {
}
