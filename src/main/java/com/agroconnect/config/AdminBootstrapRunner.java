package com.agroconnect.config;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No admin exists and BOOTSTRAP_ADMIN_USERNAME/BOOTSTRAP_ADMIN_PASSWORD are not set. " +
                     "Set these env vars to create the first admin on startup.");
            return;
        }

        User admin = User.builder()
                .username(bootstrapUsername)
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Bootstrap admin '{}' created successfully.", bootstrapUsername);
    }
}
