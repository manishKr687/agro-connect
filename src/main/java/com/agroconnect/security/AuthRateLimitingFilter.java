package com.agroconnect.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limits requests to {@code /api/auth/**} using a per-IP token bucket (Bucket4j).
 *
 * <p>Each client IP gets its own bucket. Once the bucket is empty, the filter returns
 * {@code 429 Too Many Requests} until the bucket refills at the start of the next minute.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code app.rate-limit.auth.requests-per-minute} — bucket capacity and refill rate
 *       (default: 10; dev override: 100)</li>
 * </ul>
 *
 * <p>The client IP is resolved from the {@code X-Forwarded-For} header when present
 * (for deployments behind a reverse proxy), falling back to the remote address.
 * This filter is registered before the JWT filter in {@link SecurityConfig}.
 */
@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    /** Bucket capacity and refill rate per IP per minute. */
    @Value("${app.rate-limit.auth.requests-per-minute:10}")
    private int requestsPerMinute;

    /** One bucket per client IP. ConcurrentHashMap ensures thread-safe lazy initialisation. */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Only applies to auth endpoints — all other paths are skipped. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
