package com.agroconnect.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/**
 * Fails fast when production security settings are weak or clearly misconfigured.
 */
@Component
@Profile("prod")
public class ProductionSecurityValidator {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final int MIN_BOOTSTRAP_PASSWORD_LENGTH = 16;
    private static final String DEV_SECRET = "dev-only-insecure-secret-do-not-use-in-prod";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.bootstrap.admin.username:}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    @PostConstruct
    public void validate() {
        validateJwtSecret();
        validateCorsOrigins();
        validateBootstrapAdmin();
    }

    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters in production.");
        }

        if (DEV_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must not use the development secret in production.");
        }
    }

    private void validateCorsOrigins() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain at least one HTTPS origin in production.");
        }

        for (String origin : allowedOrigins) {
            URI uri = URI.create(origin.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (!"https".equalsIgnoreCase(scheme)) {
                throw new IllegalStateException("Production CORS origins must use HTTPS: " + origin);
            }

            if (host == null || host.isBlank()) {
                throw new IllegalStateException("Production CORS origin is missing a host: " + origin);
            }

            if ("localhost".equalsIgnoreCase(host) || host.startsWith("127.")) {
                throw new IllegalStateException("Production CORS origin must not point to localhost: " + origin);
            }
        }
    }

    private void validateBootstrapAdmin() {
        boolean usernameSet = bootstrapAdminUsername != null && !bootstrapAdminUsername.isBlank();
        boolean passwordSet = bootstrapAdminPassword != null && !bootstrapAdminPassword.isBlank();

        if (usernameSet != passwordSet) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD must both be set, or both be blank.");
        }

        if (passwordSet && bootstrapAdminPassword.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 16 characters in production.");
        }
    }
}
