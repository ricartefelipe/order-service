package com.example.order.adapters.web.model;

import com.example.order.domain.model.OrderItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        @NotBlank(message = "productId is required")
        @Size(max = 100, message = "productId must have at most 100 characters")
        String productId,

        @Positive(message = "quantity must be > 0")
        @Max(value = OrderItem.MAX_QUANTITY, message = "quantity must be <= " + OrderItem.MAX_QUANTITY)
        int quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "unitPrice must be >= 0.00")
        @Digits(integer = 17, fraction = 2, message = "unitPrice must have at most 17 integer digits and 2 decimal digits")
        BigDecimal unitPrice
) {
}
