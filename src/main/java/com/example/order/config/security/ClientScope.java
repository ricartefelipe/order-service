package com.example.order.config.security;

/** Scopes a client token may carry. The authority is the scope prefixed by {@code SCOPE_}. */
public enum ClientScope {
    CREATE("orders:create"),
    READ("orders:read");

    private static final String AUTHORITY_PREFIX = "SCOPE_";

    private final String value;

    ClientScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String authority() {
        return AUTHORITY_PREFIX + value;
    }
}
