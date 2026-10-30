package com.example.order.domain.model;

import com.example.order.domain.value.Money;
import com.example.order.domain.value.Origin;
import com.example.order.domain.value.PayloadFingerprint;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An order received from one origin. Lifecycle: {@code RECEIVED -> CALCULATED}; the persisted state is
 * always {@code CALCULATED} because totals are computed before the first write. There are no later
 * transitions in this version.
 */
public record Order(
        UUID id,
        Origin origin,
        String externalOrderId,
        Currency currency,
        OrderStatus status,
        Money totalAmount,
        List<OrderItem> items,
        PayloadFingerprint fingerprint,
        Instant createdAt,
        Instant updatedAt
) {

    public static final int MAX_ITEMS = 500;

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(origin, "origin must not be null");
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("externalOrderId must not be blank");
        }
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("items must have at most " + MAX_ITEMS + " entries");
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

    public static Order newReceived(Origin origin, String externalOrderId, Currency currency, List<OrderItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        List<OrderItem> safeItems = List.copyOf(items);
        Money total = calculateTotal(safeItems);
        PayloadFingerprint fingerprint = PayloadFingerprint.of(currency, safeItems);
        return new Order(UUID.randomUUID(), origin, externalOrderId, currency, OrderStatus.RECEIVED, total,
                safeItems, fingerprint, null, null);
    }

    public Order markCalculated() {
        if (this.status == OrderStatus.CALCULATED) {
            return this;
        }
        return new Order(id, origin, externalOrderId, currency, OrderStatus.CALCULATED, totalAmount, items,
                fingerprint, createdAt, updatedAt);
    }

    public boolean hasSamePayloadAs(Order other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.fingerprint.equals(other.fingerprint);
    }

    public static Money calculateTotal(List<OrderItem> items) {
        Money total = Money.zero();
        for (OrderItem item : items) {
            total = total.add(item.lineTotal());
        }
        return total;
    }
}
