package com.example.order.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Mints HS256 client tokens for tests with the same shape the service expects in production. */
public final class ClientTokens {

    public static final String SECRET = "test-secret-with-at-least-thirty-two-bytes-0123456789";
    public static final String ISSUER = "order-service-test";
    public static final String AUDIENCE = "order-service";
    public static final String SCOPE_CREATE = "orders:create";
    public static final String SCOPE_READ = "orders:read";

    private ClientTokens() {
    }

    public static String token(String origin, String... scopes) {
        return token(origin, Instant.now().plusSeconds(600), ISSUER, AUDIENCE, scopes);
    }

    public static String expiredToken(String origin, String... scopes) {
        return token(origin, Instant.now().minusSeconds(60), ISSUER, AUDIENCE, scopes);
    }

    public static String tokenForAudience(String origin, String audience, String... scopes) {
        return token(origin, Instant.now().plusSeconds(600), ISSUER, audience, scopes);
    }

    private static String token(String origin, Instant expiresAt, String issuer, String audience, String... scopes) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(origin)
                    .issuer(issuer)
                    .audience(List.of(audience))
                    .issueTime(Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(Date.from(expiresAt))
                    .claim("scope", String.join(" ", scopes))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("could not sign test token", ex);
        }
    }
}
