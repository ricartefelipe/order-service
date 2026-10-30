package com.example.order.application.dto;

import com.example.order.domain.model.OrderStatus;

import java.util.Objects;
import java.util.Optional;

/** Optional criteria of a listing. The origin is never optional and lives outside the filter. */
public record OrderFilter(Optional<OrderStatus> status, Optional<String> externalOrderId) {

    public OrderFilter {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(externalOrderId, "externalOrderId must not be null");
    }

    public static OrderFilter none() {
        return new OrderFilter(Optional.empty(), Optional.empty());
    }
}
