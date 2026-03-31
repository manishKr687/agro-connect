package com.agroconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Servlet filter that authenticates requests by validating the JWT in the
 * {@code Authorization} header.
 *
 * <p>Skips public paths ({@code /api/auth/**}, {@code /api/public/**}) entirely so that
 * a stale or invalid token in localStorage never interferes with login or registration.
 *
 * <p>On protected requests the filter:
 * <ol>
 *   <li>Reads the {@code Authorization: Bearer <token>} header</li>
 *   <li>Rejects tokens that are blacklisted (logged out) or issued before user revocation</li>
 *   <li>Loads the user and validates token signature + expiry</li>
 *   <li>If valid, sets authentication in the {@link SecurityContextHolder}</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    /** Skip this filter for public endpoints — avoids stale-token interference on login. */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/auth/") || uri.startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;
        Instant issuedAt;

        try {
            username = jwtUtil.extractUsername(token);
            issuedAt = jwtUtil.extractIssuedAt(token);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject blacklisted tokens (explicit logout)
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject tokens issued before user was revoked (user deletion)
        if (tokenBlacklistService.isUserRevoked(username, issuedAt)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (UsernameNotFoundException ex) {
                // Token references a deleted user — treat as unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }
}
