package com.example.order.application.exception;

public class DuplicateOrderException extends RuntimeException {

    private final String externalOrderId;

    public DuplicateOrderException(String externalOrderId, Throwable cause) {
        super("Order with externalOrderId already exists: " + externalOrderId, cause);
        this.externalOrderId = externalOrderId;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }
}
