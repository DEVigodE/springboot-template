package com.template.auth.application.shared;

import java.util.List;
import java.util.function.Function;

public record PaginatedResult<T>(List<T> items, long total, int page, int limit, int totalPages) {

    public static <T> PaginatedResult<T> of(List<T> items, long total, PaginationParams params) {
        int totalPages = (int) Math.ceil((double) total / params.limit());
        return new PaginatedResult<>(List.copyOf(items), total, params.page(), params.limit(), totalPages);
    }

    public <R> PaginatedResult<R> map(Function<T, R> mapper) {
        return new PaginatedResult<>(items.stream().map(mapper).toList(), total, page, limit, totalPages);
    }
}
