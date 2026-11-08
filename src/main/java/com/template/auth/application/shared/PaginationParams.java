package com.template.auth.application.shared;

/** 1-based page, bounded page size. */
public record PaginationParams(int page, int limit) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    public PaginationParams {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    public static PaginationParams of(Integer page, Integer limit) {
        return new PaginationParams(page == null ? 1 : page, limit == null ? DEFAULT_LIMIT : limit);
    }

    /** Zero-based page index, as Spring Data and SQL OFFSET expect. */
    public int zeroBasedPage() {
        return page - 1;
    }
}
