package com.example.order.application.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record CreateOrderItemCommand(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {

    public CreateOrderItemCommand {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be >= 0");
        }
    }
}
