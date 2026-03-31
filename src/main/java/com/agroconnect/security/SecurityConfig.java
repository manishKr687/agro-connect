package com.agroconnect.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the AgroConnect API.
 *
 * <p>Key decisions:
 * <ul>
 *   <li><b>Stateless sessions</b> — no server-side session; every request must carry a valid JWT.</li>
 *   <li><b>CSRF disabled</b> — safe because the API is stateless and not accessed via browser forms.</li>
 *   <li><b>Public endpoints</b> — {@code /api/auth/**} and {@code /api/public/**} are permit-all;
 *       every other path requires authentication.</li>
 *   <li><b>Filter order</b> — {@link AuthRateLimitingFilter} runs before {@link JwtAuthenticationFilter}
 *       so rate-limited requests are rejected before any DB lookup occurs.</li>
 *   <li><b>JWT filter skip</b> — {@link JwtAuthenticationFilter} skips public paths entirely,
 *       so a stale token in localStorage never interferes with login.</li>
 *   <li><b>CORS origins</b> — configured via {@code app.cors.allowed-origins} (comma-separated),
 *       set per-profile (e.g. {@code http://localhost:3000} in dev).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitingFilter authRateLimitingFilter;
    private final SseRateLimitingFilter sseRateLimitingFilter;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthRateLimitingFilter authRateLimitingFilter,
                          SseRateLimitingFilter sseRateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRateLimitingFilter = authRateLimitingFilter;
        this.sseRateLimitingFilter = sseRateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(authRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(sseRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
