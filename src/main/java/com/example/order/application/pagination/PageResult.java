package com.example.order.application.pagination;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        Objects.requireNonNull(content, "content must not be null");
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must be >= 0");
        }
    }

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements, int totalPages) {
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }
}
