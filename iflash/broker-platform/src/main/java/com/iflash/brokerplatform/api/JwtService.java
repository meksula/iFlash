package com.iflash.brokerplatform.api;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and validates the stateless JWT that authenticates API calls. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    JwtService(@Value("${ibp.jwt.secret}") String secret,
               @Value("${ibp.jwt.ttl}") Duration ttl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public String issue(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                   .subject(String.valueOf(userId))
                   .claim("email", email)
                   .issuedAt(Date.from(now))
                   .expiration(Date.from(now.plus(ttl)))
                   .signWith(key)
                   .compact();
    }

    /** @return the user id encoded in the token, or {@code null} if it is missing/invalid/expired. */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
