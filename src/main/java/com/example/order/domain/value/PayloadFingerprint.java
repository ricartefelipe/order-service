package com.example.order.domain.value;

import com.example.order.domain.model.OrderItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Versioned SHA-256 of the canonical order payload. Two requests with the same external id are the
 * same order only when their fingerprints match; otherwise they conflict.
 *
 * <p>Canonical form (version {@value #CURRENT_VERSION}): the version, the currency code and one line
 * per item ({@code productId<TAB>quantity<TAB>unitPrice}), lines sorted bytewise, joined by {@code \n}.
 * The Liquibase changeset {@code 002} computes the same text in SQL for orders stored before this
 * version; both sides must change together.
 */
public record PayloadFingerprint(String version, String hash) {

    public static final String CURRENT_VERSION = "v1";
    private static final String LINE_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "\t";
    private static final int SHA256_HEX_LENGTH = 64;

    public PayloadFingerprint {
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(hash, "hash must not be null");
        if (version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        if (hash.length() != SHA256_HEX_LENGTH) {
            throw new IllegalArgumentException("hash must be a SHA-256 hex digest");
        }
    }

    public static PayloadFingerprint of(Currency currency, List<OrderItem> items) {
        return new PayloadFingerprint(CURRENT_VERSION, sha256Hex(canonical(currency, items)));
    }

    static String canonical(Currency currency, List<OrderItem> items) {
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(items, "items must not be null");
        String lines = items.stream()
                .map(PayloadFingerprint::canonicalLine)
                .sorted()
                .collect(Collectors.joining(LINE_SEPARATOR));
        return CURRENT_VERSION + LINE_SEPARATOR + currency.getCurrencyCode() + LINE_SEPARATOR + lines;
    }

    private static String canonicalLine(OrderItem item) {
        return item.productId() + FIELD_SEPARATOR + item.quantity() + FIELD_SEPARATOR
                + item.unitPrice().toBigDecimal().toPlainString();
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
