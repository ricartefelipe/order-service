package com.example.order.application.dto;

import com.example.order.domain.value.Origin;

import java.util.Currency;
import java.util.List;
import java.util.Objects;

public record CreateOrderCommand(
        Origin origin,
        String externalOrderId,
        Currency currency,
        List<CreateOrderItemCommand> items
) {

    public CreateOrderCommand {
        Objects.requireNonNull(origin, "origin must not be null");
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("externalOrderId must not be blank");
        }
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        items = List.copyOf(items);
    }
}
