package com.example.lms.auth;

import com.example.lms.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Decoders.BASE64.decode(properties.secretBase64());
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("The JWT signing key must contain at least 32 bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public IssuedToken issueAccessToken(User user) {
        return issue(user, ACCESS_TOKEN, properties.accessTokenTtl());
    }

    public IssuedToken issueRefreshToken(User user) {
        return issue(user, REFRESH_TOKEN, properties.refreshTokenTtl());
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!ACCESS_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new io.jsonwebtoken.JwtException("Invalid access token.");
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!REFRESH_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new io.jsonwebtoken.JwtException("Invalid refresh token.");
        }
        return claims;
    }

    private IssuedToken issue(User user, String tokenType, java.time.Duration ttl) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(ttl);
        String value = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(value, expiresAt);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
