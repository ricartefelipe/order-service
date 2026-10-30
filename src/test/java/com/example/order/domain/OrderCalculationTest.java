package com.example.order.domain;

import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.model.OrderStatus;
import com.example.order.domain.value.Money;
import com.example.order.domain.value.Origin;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderCalculationTest {

    private static final Origin ORIGIN = Origin.of("system-a");
    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    void shouldCalculateLineTotalAndTotalAmount_withScale2() {
        OrderItem item1 = OrderItem.of("SKU-1", 2, Money.of(new BigDecimal("10.50")));
        OrderItem item2 = OrderItem.of("SKU-2", 1, Money.of(new BigDecimal("5.00")));

        Order order = Order.newReceived(ORIGIN, "A-123456", BRL, List.of(item1, item2)).markCalculated();

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

    @Test
    void shouldRejectQuantityAboveTheMaximum() {
        Money price = Money.of(new BigDecimal("1.00"));
        assertThrows(IllegalArgumentException.class,
                () -> OrderItem.of("SKU-1", OrderItem.MAX_QUANTITY + 1, price));
    }

    @Test
    void shouldAcceptQuantityAtTheMaximum() {
        OrderItem item = OrderItem.of("SKU-1", OrderItem.MAX_QUANTITY, Money.of(new BigDecimal("0.01")));
        assertEquals(new BigDecimal("10000.00"), item.lineTotal().toBigDecimal());
    }

    @Test
    void shouldRejectAmountsBeyondTheColumnCapacity() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(Money.MAX_AMOUNT.add(new BigDecimal("0.01"))));
    }

    @Test
    void shouldRejectTotalOverflow_whenLineTotalsSaturate() {
        Money nearMax = Money.of(new BigDecimal("99999999999999999.00"));
        OrderItem item1 = OrderItem.of("SKU-1", 1, nearMax);
        OrderItem item2 = OrderItem.of("SKU-2", 1, Money.of(new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> Order.newReceived(ORIGIN, "A-1", BRL, List.of(item1, item2)));
    }

    @Test
    void shouldRejectMoreItemsThanTheMaximum() {
        Money price = Money.of(new BigDecimal("1.00"));
        List<OrderItem> items = java.util.stream.IntStream.rangeClosed(0, Order.MAX_ITEMS)
                .mapToObj(i -> OrderItem.of("SKU-" + i, 1, price))
                .toList();
        assertThrows(IllegalArgumentException.class, () -> Order.newReceived(ORIGIN, "A-1", BRL, items));
    }

    @Test
    void shouldRejectBlankOrigin() {
        assertThrows(IllegalArgumentException.class, () -> Origin.of("  "));
    }
}
