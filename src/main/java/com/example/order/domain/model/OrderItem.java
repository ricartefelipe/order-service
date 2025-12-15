package com.example.order.domain.model;

import com.example.order.domain.value.Money;

import java.util.Objects;

public record OrderItem(
        String productId,
        int quantity,
        Money unitPrice,
        Money lineTotal
) {

    public OrderItem {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(lineTotal, "lineTotal must not be null");
        if (unitPrice.isNegative()) {
            throw new IllegalArgumentException("unitPrice must not be negative");
        }
        if (lineTotal.isNegative()) {
            throw new IllegalArgumentException("lineTotal must not be negative");
        }
    }

    public static OrderItem of(String productId, int quantity, Money unitPrice) {
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Money lineTotal = unitPrice.multiply(quantity);
        return new OrderItem(productId, quantity, unitPrice, lineTotal);
    }
}
