package com.agroconnect.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limits requests to {@code /api/auth/**} using per-IP token buckets (Bucket4j).
 *
 * <p>Additionally, for {@code POST /api/auth/login}, a separate per-username bucket
 * is checked so that distributed brute-force attacks (many IPs, one target username)
 * are also blocked.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code app.rate-limit.auth.requests-per-minute} - per-IP bucket (default: 10)</li>
 *   <li>{@code app.rate-limit.auth.username-requests-per-minute} - per-username bucket (default: 5)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.auth.requests-per-minute:10}")
    private int ipRequestsPerMinute;

    @Value("${app.rate-limit.auth.username-requests-per-minute:5}")
    private int usernameRequestsPerMinute;

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> usernameBuckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        if (!ipBuckets.computeIfAbsent(ip, k -> newBucket(ipRequestsPerMinute)).tryConsume(1)) {
            rejectTooManyRequests(response);
            return;
        }

        if (request.getRequestURI().endsWith("/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
            String username = extractUsername(wrapped);

            if (username != null && !username.isBlank()
                    && !usernameBuckets.computeIfAbsent(username, k -> newBucket(usernameRequestsPerMinute)).tryConsume(1)) {
                rejectTooManyRequests(response);
                return;
            }

            filterChain.doFilter(wrapped, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractUsername(CachedBodyHttpServletRequest request) {
        try {
            JsonNode node = objectMapper.readTree(request.getCachedBody());
            JsonNode usernameNode = node.get("username");
            return usernameNode != null ? usernameNode.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Bucket newBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private void rejectTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
