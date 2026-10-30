package com.example.order.application.pagination;

import java.util.Objects;

public record Sort(SortProperty property, SortDirection direction) {

    public Sort {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
    }
}
