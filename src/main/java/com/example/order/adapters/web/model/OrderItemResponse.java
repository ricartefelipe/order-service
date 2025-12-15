package com.example.order.adapters.web.model;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
