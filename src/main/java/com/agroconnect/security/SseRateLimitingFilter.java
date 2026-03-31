package com.agroconnect.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limits new SSE connection attempts to {@code /api/public/demands/stream}.
 *
 * <p>Uses a per-IP token bucket to cap how many new connections an IP can open per minute.
 * This works alongside the concurrent connection limit in
 * {@link com.agroconnect.service.DemandEventService} to cover both attack vectors:
 * rapid reconnects and holding many connections open simultaneously.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code app.sse.connections-per-minute} — new connection attempts per IP per minute (default: 10)</li>
 * </ul>
 */
@Component
public class SseRateLimitingFilter extends OncePerRequestFilter {

    private static final String SSE_PATH = "/api/public/demands/stream";

    @Value("${app.sse.connections-per-minute:10}")
    private int connectionsPerMinute;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().equals(SSE_PATH);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(connectionsPerMinute)
                        .refillGreedy(connectionsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build());

        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many SSE connection attempts. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
