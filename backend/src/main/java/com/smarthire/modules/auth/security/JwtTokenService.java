package com.smarthire.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthenticatedUser authenticatedUser) {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(String.valueOf(authenticatedUser.userId()))
            .claim("email", authenticatedUser.email())
            .claim("fullName", authenticatedUser.fullName())
            .claim("roles", authenticatedUser.roles())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds())))
            .signWith(signingKey)
            .compact();
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        List<String> roles = claims.get("roles", List.class);

        return new AuthenticatedUser(
            Long.valueOf(claims.getSubject()),
            claims.get("email", String.class),
            claims.get("fullName", String.class),
            roles
        );
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationSeconds();
    }
}
