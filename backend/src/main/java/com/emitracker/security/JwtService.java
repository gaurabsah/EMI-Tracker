package com.emitracker.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.emitracker.config.AppProperties;
import com.emitracker.user.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiry;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expiry = Duration.ofMinutes(props.jwt().expiryMinutes());
        this.clock = clock;
    }

    public String issue(AuthPrincipal principal) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .subject(Long.toString(principal.userId()))
                .claim("role", principal.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)));
        if (principal.companyId() != null) {
            builder.claim("companyId", principal.companyId());
        }
        if (principal.customerId() != null) {
            builder.claim("customerId", principal.customerId());
        }
        return builder.signWith(key).compact();
    }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant()))
                    .build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthPrincipal(
                    Long.parseLong(claims.getSubject()),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.get("companyId", Long.class),
                    claims.get("customerId", Long.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
