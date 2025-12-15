package com.example.order.application.dto;

import java.util.List;
import java.util.Objects;

public record CreateOrderCommand(
        String externalOrderId,
        List<CreateOrderItemCommand> items
) {

    public CreateOrderCommand {
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("externalOrderId must not be blank");
        }
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        items = List.copyOf(items);
    }
}
