package com.template.auth.infrastructure.persistence.shared;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageMapper {

    private PageMapper() {
    }

    public static Pageable toPageable(PaginationParams params, Sort sort) {
        return PageRequest.of(params.zeroBasedPage(), params.limit(), sort);
    }

    public static <T, R> PaginatedResult<R> toResult(Page<T> page, PaginationParams params,
            Function<T, R> mapper) {
        return PaginatedResult.of(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                params);
    }
}
