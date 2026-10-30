package com.example.order.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object for monetary values.
 *
 * Invariants:
 * - Always scaled to 2 decimal places using HALF_UP.
 * - Never above {@link #MAX_AMOUNT}, the largest value the {@code numeric(19,2)} columns can hold.
 */
public record Money(BigDecimal amount) {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999999.99");

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        amount = amount.setScale(SCALE, ROUNDING_MODE);
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("amount must be <= " + MAX_AMOUNT.toPlainString());
        }
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("multiplier must be >= 0");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public BigDecimal toBigDecimal() {
        return amount;
    }
}
