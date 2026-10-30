package com.example.order.domain.value;

import com.example.order.domain.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PayloadFingerprintTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    private static OrderItem item(String productId, int quantity, String unitPrice) {
        return OrderItem.of(productId, quantity, Money.of(new BigDecimal(unitPrice)));
    }

    @Test
    void sameItemsInDifferentOrderProduceTheSameFingerprint() {
        PayloadFingerprint a = PayloadFingerprint.of(BRL, List.of(item("SKU-1", 2, "10.50"), item("SKU-2", 1, "5.00")));
        PayloadFingerprint b = PayloadFingerprint.of(BRL, List.of(item("SKU-2", 1, "5.00"), item("SKU-1", 2, "10.50")));

        assertEquals(a, b);
        assertEquals(PayloadFingerprint.CURRENT_VERSION, a.version());
    }

    @Test
    void priceWrittenWithDifferentScaleIsTheSamePayload() {
        PayloadFingerprint a = PayloadFingerprint.of(BRL, List.of(item("SKU-1", 1, "5")));
        PayloadFingerprint b = PayloadFingerprint.of(BRL, List.of(item("SKU-1", 1, "5.00")));

        assertEquals(a, b);
    }

    @Test
    void differentQuantityPriceCurrencyOrProductChangeTheFingerprint() {
        PayloadFingerprint base = PayloadFingerprint.of(BRL, List.of(item("SKU-1", 2, "10.50")));

        assertNotEquals(base, PayloadFingerprint.of(BRL, List.of(item("SKU-1", 3, "10.50"))));
        assertNotEquals(base, PayloadFingerprint.of(BRL, List.of(item("SKU-1", 2, "10.51"))));
        assertNotEquals(base, PayloadFingerprint.of(USD, List.of(item("SKU-1", 2, "10.50"))));
        assertNotEquals(base, PayloadFingerprint.of(BRL, List.of(item("SKU-9", 2, "10.50"))));
    }

    @Test
    void duplicatedLineIsNotTheSameAsASingleLine() {
        PayloadFingerprint once = PayloadFingerprint.of(BRL, List.of(item("SKU-1", 1, "1.00")));
        PayloadFingerprint twice = PayloadFingerprint.of(BRL, List.of(item("SKU-1", 1, "1.00"), item("SKU-1", 1, "1.00")));

        assertNotEquals(once, twice);
    }

    @Test
    void canonicalTextIsVersionCurrencyAndSortedTabSeparatedLines() {
        String canonical = PayloadFingerprint.canonical(BRL, List.of(item("SKU-2", 1, "5.00"), item("SKU-1", 2, "10.50")));

        assertEquals("v1\nBRL\nSKU-1\t2\t10.50\nSKU-2\t1\t5.00", canonical);
    }
}
