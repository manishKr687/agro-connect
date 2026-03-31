package com.agroconnect.config;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures a bootstrap admin account exists on application startup.
 *
 * <p>Behaviour:
 * <ul>
 *   <li><b>No admin with the bootstrap username</b> — creates the account.</li>
 *   <li><b>Admin exists + {@code app.bootstrap.admin.reset-password=true}</b> — updates the
 *       password if it differs. Only enable this in dev/staging; never in production.</li>
 *   <li><b>Admin exists + reset disabled (default)</b> — no-op, existing password preserved.</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code app.bootstrap.admin.username}</li>
 *   <li>{@code app.bootstrap.admin.password}</li>
 *   <li>{@code app.bootstrap.admin.reset-password} — default {@code false}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:}")
    private String bootstrapUsername;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapPassword;

    /** Only true in dev — allows password sync on restart. Never set in prod. */
    @Value("${app.bootstrap.admin.reset-password:false}")
    private boolean resetPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("BOOTSTRAP_ADMIN_USERNAME / BOOTSTRAP_ADMIN_PASSWORD not set — skipping admin bootstrap.");
            return;
        }

        userRepository.findByUsername(bootstrapUsername).ifPresentOrElse(
            (@NonNull User existing) -> {
                if (resetPassword && !passwordEncoder.matches(bootstrapPassword, existing.getPassword())) {
                    existing.setPassword(passwordEncoder.encode(bootstrapPassword));
                    userRepository.save(existing);
                    log.info("Bootstrap admin '{}' password reset from config.", bootstrapUsername);
                }
            },
            () -> {
                User admin = User.builder()
                        .username(bootstrapUsername)
                        .password(passwordEncoder.encode(bootstrapPassword))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Bootstrap admin '{}' created successfully.", bootstrapUsername);
            }
        );
    }
}
