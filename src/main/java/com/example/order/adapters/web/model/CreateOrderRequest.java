package com.example.order.adapters.web.model;

import com.example.order.domain.model.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "externalOrderId is required")
        @Size(max = 100, message = "externalOrderId must have at most 100 characters")
        String externalOrderId,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be an ISO 4217 code with 3 uppercase letters")
        String currency,

        @NotEmpty(message = "items must not be empty")
        @Size(max = Order.MAX_ITEMS, message = "items must have at most " + Order.MAX_ITEMS + " entries")
        List<@Valid CreateOrderItemRequest> items
) {
}
