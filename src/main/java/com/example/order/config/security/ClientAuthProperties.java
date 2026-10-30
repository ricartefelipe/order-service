package com.example.order.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Machine-to-machine authentication. Tokens are JWTs issued to integrating systems; the service checks
 * issuer, audience, expiration and signature, then uses {@code sub} as the origin and {@code scope}
 * for authorization. Exactly one key source must be configured: a JWKS endpoint of an identity
 * provider (client credentials flow) or a shared HMAC secret for installations without one.
 */
@ConfigurationProperties(prefix = "orders.auth")
public record ClientAuthProperties(String issuer, String audience, String jwksUri, String secret) {

    public static final int MIN_SECRET_BYTES = 32;

    public ClientAuthProperties {
        if (isBlank(issuer)) {
            throw new IllegalArgumentException("orders.auth.issuer is required");
        }
        if (isBlank(audience)) {
            throw new IllegalArgumentException("orders.auth.audience is required");
        }
        boolean hasJwks = !isBlank(jwksUri);
        boolean hasSecret = !isBlank(secret);
        if (hasJwks == hasSecret) {
            throw new IllegalArgumentException("configure exactly one of orders.auth.jwks-uri or orders.auth.secret");
        }
        if (hasSecret && secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("orders.auth.secret must have at least " + MIN_SECRET_BYTES + " bytes");
        }
    }

    public boolean usesJwks() {
        return !isBlank(jwksUri);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
