package com.example.order.application.exception;

/** The external id was already used by the same origin with a different payload. */
public class IdempotencyConflictException extends RuntimeException {

    private final String externalOrderId;

    public IdempotencyConflictException(String externalOrderId) {
        super("Order with externalOrderId " + externalOrderId + " already exists with a different payload");
        this.externalOrderId = externalOrderId;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }
}
