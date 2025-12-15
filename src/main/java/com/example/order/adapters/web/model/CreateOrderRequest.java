package com.example.order.adapters.web.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "externalOrderId is required")
        String externalOrderId,

        @NotEmpty(message = "items must not be empty")
        List<@Valid CreateOrderItemRequest> items
) {
}
