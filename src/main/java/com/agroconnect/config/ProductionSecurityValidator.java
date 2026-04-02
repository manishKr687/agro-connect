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

    @Value("${app.jwt.issuer:}")
    private String jwtIssuer;

    @Value("${app.bootstrap.admin.phone-number:}")
    private String bootstrapAdminPhoneNumber;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    @Value("${app.password-reset.mail.enabled:false}")
    private boolean passwordResetMailEnabled;

    @Value("${app.password-reset.mail.from:}")
    private String passwordResetMailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.password-reset.sms.enabled:false}")
    private boolean passwordResetSmsEnabled;

    @Value("${app.password-reset.sms.provider:TWILIO}")
    private String smsProvider;

    @Value("${app.password-reset.sms.from:}")
    private String smsFrom;

    @Value("${app.password-reset.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.password-reset.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @PostConstruct
    public void validate() {
        validateJwtSecret();
        validateJwtIssuer();
        validateCorsOrigins();
        validateBootstrapAdmin();
        validatePasswordResetMail();
        validatePasswordResetSms();
    }

    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters in production.");
        }

        if (DEV_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must not use the development secret in production.");
        }
    }

    private void validateJwtIssuer() {
        if (jwtIssuer == null || jwtIssuer.isBlank()) {
            throw new IllegalStateException("JWT_ISSUER must be set in production.");
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
        boolean phoneNumberSet = bootstrapAdminPhoneNumber != null && !bootstrapAdminPhoneNumber.isBlank();
        boolean passwordSet = bootstrapAdminPassword != null && !bootstrapAdminPassword.isBlank();

        if (phoneNumberSet != passwordSet) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PHONE_NUMBER and BOOTSTRAP_ADMIN_PASSWORD must both be set, or both be blank.");
        }

        if (passwordSet && bootstrapAdminPassword.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 16 characters in production.");
        }
    }

    private void validatePasswordResetMail() {
        if (!passwordResetMailEnabled) {
            return;
        }

        if (passwordResetMailFrom == null || passwordResetMailFrom.isBlank()) {
            throw new IllegalStateException("PASSWORD_RESET_MAIL_FROM must be set when email password reset delivery is enabled.");
        }

        if (mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException("MAIL_HOST must be set when email password reset delivery is enabled.");
        }

        if (mailUsername == null || mailUsername.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME must be set when email password reset delivery is enabled.");
        }
    }

    private void validatePasswordResetSms() {
        if (!passwordResetSmsEnabled) {
            return;
        }

        if (!"TWILIO".equalsIgnoreCase(smsProvider)) {
            throw new IllegalStateException("Only TWILIO is supported for PASSWORD_RESET_SMS_PROVIDER right now.");
        }

        if (smsFrom == null || smsFrom.isBlank()) {
            throw new IllegalStateException("PASSWORD_RESET_SMS_FROM must be set when SMS password reset delivery is enabled.");
        }

        if (twilioAccountSid == null || twilioAccountSid.isBlank()) {
            throw new IllegalStateException("TWILIO_ACCOUNT_SID must be set when SMS password reset delivery is enabled.");
        }

        if (twilioAuthToken == null || twilioAuthToken.isBlank()) {
            throw new IllegalStateException("TWILIO_AUTH_TOKEN must be set when SMS password reset delivery is enabled.");
        }
    }
}
