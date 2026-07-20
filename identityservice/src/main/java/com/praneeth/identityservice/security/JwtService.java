package com.praneeth.identityservice.security;

import com.praneeth.identityservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    private final String jwtSecret;
    private final long jwtAccessTokenExpiration;

    public JwtService(
            @Value("${application.security.jwt.secret}")
            String jwtSecret,
            @Value("${application.security.jwt.access-token-expiration}")
            long jwtAccessTokenExpiration
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtAccessTokenExpiration = jwtAccessTokenExpiration;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusMillis(jwtAccessTokenExpiration)
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public boolean isTokenValid(
            String token,
            User user
    ) {
        String email = extractEmail(token);

        return email.equalsIgnoreCase(user.getEmail())
                && !isTokenExpired(token)
                && user.isEnabled();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = extractClaim(
                token,
                Claims::getExpiration
        );

        return expiration.before(new Date());
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtAccessTokenExpiration / 1000;
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimResolver
    ) {
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
