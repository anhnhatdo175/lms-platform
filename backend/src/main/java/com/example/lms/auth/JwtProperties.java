package com.example.lms.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secretBase64,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {
    public JwtProperties {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException("app.jwt.secret-base64 must be configured.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("app.jwt.issuer must be configured.");
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("app.jwt.access-token-ttl must be positive.");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("app.jwt.refresh-token-ttl must be positive.");
        }
    }
}
