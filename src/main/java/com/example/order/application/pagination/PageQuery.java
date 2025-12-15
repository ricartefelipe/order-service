package com.example.order.application.pagination;

import java.util.List;
import java.util.Objects;

public record PageQuery(int page, int size, List<Sort> sorts) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be <= " + MAX_SIZE);
        }
        Objects.requireNonNull(sorts, "sorts must not be null");
        sorts = List.copyOf(sorts);
    }

    public static PageQuery of(int page, int size, List<Sort> sorts) {
        return new PageQuery(page, size, sorts);
    }
}
