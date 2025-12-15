package com.example.order.application.pagination;

import java.util.Objects;

public record Sort(String property, SortDirection direction) {

    public Sort {
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("property must not be blank");
        }
        Objects.requireNonNull(direction, "direction must not be null");
    }
}
