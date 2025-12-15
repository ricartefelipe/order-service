package com.example.order.domain.model;

import com.example.order.domain.value.Money;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Order(
        UUID id,
        String externalOrderId,
        OrderStatus status,
        Money totalAmount,
        List<OrderItem> items,
        Instant createdAt,
        Instant updatedAt
) {

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("externalOrderId must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        items = List.copyOf(items);
        if (totalAmount.isNegative()) {
            throw new IllegalArgumentException("totalAmount must not be negative");
        }

        Money calculated = calculateTotal(items);
        if (!calculated.amount().equals(totalAmount.amount())) {
            throw new IllegalArgumentException("totalAmount must match sum(lineTotal)");
        }
    }

    public static Order newReceived(String externalOrderId, List<OrderItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        List<OrderItem> safeItems = List.copyOf(items);
        Money total = calculateTotal(safeItems);
        return new Order(UUID.randomUUID(), externalOrderId, OrderStatus.RECEIVED, total, safeItems, null, null);
    }

    public Order markCalculated() {
        if (this.status == OrderStatus.CALCULATED) {
            return this;
        }
        return new Order(this.id, this.externalOrderId, OrderStatus.CALCULATED, this.totalAmount, this.items, this.createdAt, this.updatedAt);
    }

    public static Money calculateTotal(List<OrderItem> items) {
        Money total = Money.zero();
        for (OrderItem item : items) {
            total = total.add(item.lineTotal());
        }
        return total;
    }
}
