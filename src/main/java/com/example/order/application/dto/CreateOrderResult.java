package com.example.order.application.dto;

import com.example.order.domain.model.Order;

import java.util.Objects;

public record CreateOrderResult(Order order, boolean created) {

    public CreateOrderResult {
        Objects.requireNonNull(order, "order must not be null");
    }
}
