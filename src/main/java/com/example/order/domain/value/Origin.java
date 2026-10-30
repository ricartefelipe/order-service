package com.example.order.domain.value;

import java.util.Objects;

/**
 * Identity of the integrating system that owns an order. Every order belongs to exactly one origin
 * and is never visible to another one.
 */
public record Origin(String value) {

    public static final int MAX_LENGTH = 100;

    public Origin {
        Objects.requireNonNull(value, "origin must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("origin must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("origin must have at most " + MAX_LENGTH + " characters");
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("origin must not contain control characters");
        }
    }

    public static Origin of(String value) {
        return new Origin(value);
    }
}
