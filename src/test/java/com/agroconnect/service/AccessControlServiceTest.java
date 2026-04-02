package com.agroconnect.service;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks AccessControlService accessControlService;

    private void authenticateAs(String phoneNumber) {
        var auth = new UsernamePasswordAuthenticationToken(phoneNumber, null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── requireAdmin ──────────────────────────────────────────────────────────

    @Test
    void requireAdmin_adminUser_returnsUser() {
        User admin = User.builder().id(1L).name("Admin").phoneNumber("+919999999999").role(Role.ADMIN).build();
        authenticateAs("+919999999999");
        when(userRepository.findByPhoneNumber("+919999999999")).thenReturn(Optional.of(admin));

        User result = accessControlService.requireAdmin(1L);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void requireAdmin_nonAdminUser_throwsForbidden() {
        User farmer = User.builder().id(2L).name("Farmer One").phoneNumber("+919876543210").role(Role.FARMER).build();
        authenticateAs("+919876543210");
        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(farmer));

        assertThatThrownBy(() -> accessControlService.requireAdmin(2L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── requireCurrentUser ────────────────────────────────────────────────────

    @Test
    void requireCurrentUser_correctIdAndRole_returnsUser() {
        User agent = User.builder().id(3L).name("Agent One").phoneNumber("+919876543211").role(Role.AGENT).build();
        authenticateAs("+919876543211");
        when(userRepository.findByPhoneNumber("+919876543211")).thenReturn(Optional.of(agent));

        User result = accessControlService.requireCurrentUser(3L, Role.AGENT);

        assertThat(result.getId()).isEqualTo(3L);
    }

    @Test
    void requireCurrentUser_wrongUserId_throwsForbidden() {
        User agent = User.builder().id(3L).name("Agent One").phoneNumber("+919876543211").role(Role.AGENT).build();
        authenticateAs("+919876543211");
        when(userRepository.findByPhoneNumber("+919876543211")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> accessControlService.requireCurrentUser(99L, Role.AGENT))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requireCurrentUser_wrongRole_throwsForbidden() {
        User farmer = User.builder().id(4L).name("Farmer One").phoneNumber("+919876543212").role(Role.FARMER).build();
        authenticateAs("+919876543212");
        when(userRepository.findByPhoneNumber("+919876543212")).thenReturn(Optional.of(farmer));

        assertThatThrownBy(() -> accessControlService.requireCurrentUser(4L, Role.AGENT))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
