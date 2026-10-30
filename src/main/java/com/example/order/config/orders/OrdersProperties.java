package com.example.order.config.orders;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Currency;

/** Business defaults of the service. */
@ConfigurationProperties(prefix = "orders")
public record OrdersProperties(Currency defaultCurrency) {

    public OrdersProperties {
        if (defaultCurrency == null) {
            throw new IllegalArgumentException("orders.default-currency is required (ISO 4217 code)");
        }
    }
}
