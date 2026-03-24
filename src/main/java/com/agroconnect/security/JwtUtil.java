package com.agroconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility for generating and validating HMAC-SHA signed JWTs.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code app.jwt.secret} — signing key (must be at least 32 characters; injected from env/profile)</li>
 *   <li>{@code app.jwt.expiration-ms} — token lifetime in milliseconds (default: 10 hours)</li>
 * </ul>
 *
 * <p>The signing key is derived from the secret string via {@link Keys#hmacShaKeyFor} in {@link #init()},
 * which runs once after the bean is constructed. All token operations use jjwt 0.12.x APIs.
 */
@Component
public class JwtUtil {

    /** Injected from {@code app.jwt.secret}. Must be at least 32 bytes for HMAC-SHA256. */
    @Value("${app.jwt.secret}")
    private String secret;

    /** Token lifetime in milliseconds. Defaults to 36,000,000 ms (10 hours). */
    @Value("${app.jwt.expiration-ms:36000000}")
    private long expirationMs;

    private SecretKey signingKey;

    /** Derives the HMAC signing key from the configured secret string after Spring injection. */
    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT with the username as subject and a configured expiry.
     *
     * @param userDetails the authenticated user
     * @return compact serialised JWT string
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return userDetails.getUsername().equals(extractUsername(token)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
