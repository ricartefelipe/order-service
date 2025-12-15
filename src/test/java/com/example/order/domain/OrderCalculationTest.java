package com.example.order.domain;

import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.value.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCalculationTest {

    @Test
    void shouldCalculateLineTotalAndTotalAmount_withScale2() {
        OrderItem item1 = OrderItem.of("SKU-1", 2, Money.of(new BigDecimal("10.50")));
        OrderItem item2 = OrderItem.of("SKU-2", 1, Money.of(new BigDecimal("5.00")));

        Order order = Order.newReceived("A-123456", List.of(item1, item2)).markCalculated();

        assertEquals(OrderStatus.CALCULATED, order.status());
        assertEquals(new BigDecimal("21.00"), item1.lineTotal().toBigDecimal());
        assertEquals(new BigDecimal("5.00"), item2.lineTotal().toBigDecimal());
        assertEquals(new BigDecimal("26.00"), order.totalAmount().toBigDecimal());
    }

    @Test
    void shouldApplyHalfUpRounding() {
        Money m = Money.of(new BigDecimal("10.005"));
        assertEquals(new BigDecimal("10.01"), m.toBigDecimal());
    }
}
