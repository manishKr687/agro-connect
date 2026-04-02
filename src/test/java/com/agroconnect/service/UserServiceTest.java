package com.agroconnect.service;

import com.agroconnect.dto.LoginRequest;
import com.agroconnect.dto.RegisterUserRequest;
import com.agroconnect.dto.ChangePasswordRequest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import com.agroconnect.security.LoginAttemptService;
import com.agroconnect.security.RefreshTokenService;
import com.agroconnect.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AccessControlService accessControlService;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock LoginAttemptService loginAttemptService;
    @Mock RefreshTokenService refreshTokenService;

    @InjectMocks UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(loginAttemptService.isLocked(anyString())).thenReturn(false);
        lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void register_farmerRole_succeeds() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setName("Farmer One");
        request.setPhoneNumber("+919876543210");
        request.setPassword("password123");
        request.setEmail("farmer1@example.com");
        request.setRole(Role.FARMER);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(request);

        assertThat(result.getName()).isEqualTo("Farmer One");
        assertThat(result.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(result.getEmail()).isEqualTo("farmer1@example.com");
        assertThat(result.getRole()).isEqualTo(Role.FARMER);
    }

    @Test
    void register_retailerRole_succeeds() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setName("Retailer One");
        request.setPhoneNumber("+919876543210");
        request.setPassword("password123");
        request.setRole(Role.RETAILER);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(request);

        assertThat(result.getName()).isEqualTo("Retailer One");
        assertThat(result.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(result.getRole()).isEqualTo(Role.RETAILER);
    }

    @Test
    void register_adminRole_throwsForbidden() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setName("Sneaky");
        request.setPhoneNumber("+919876543210");
        request.setPassword("password123");
        request.setEmail("admin@example.com");
        request.setRole(Role.ADMIN);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_agentRole_throwsForbidden() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setName("Sneaky");
        request.setPhoneNumber("+919876543210");
        request.setPassword("password123");
        request.setRole(Role.AGENT);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicatePhoneNumber_throwsConflict() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setName("Farmer Two");
        request.setPhoneNumber("+919876543210");
        request.setPassword("password123");
        request.setRole(Role.FARMER);

        when(userRepository.findByPhoneNumber("+919876543210"))
                .thenReturn(Optional.of(User.builder().id(99L).name("Existing").phoneNumber("+919876543210").build()));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("+919876543210");
        request.setPassword("wrongpassword");

        User user = User.builder().name("Farmer One").phoneNumber("+919876543210").password("hashed").build();
        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unknownPhoneNumber_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("+911234567890");
        request.setPassword("password123");

        when(userRepository.findByPhoneNumber("+911234567890")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePassword_validCurrentPassword_updatesPasswordAndRevokesSessions() {
        User user = User.builder()
                .id(1L)
                .name("Farmer One")
                .phoneNumber("+919876543210")
                .password("old-hash")
                .role(Role.FARMER)
                .build();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password-123");

        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password-123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");

        userService.changePassword(user, request);

        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(tokenBlacklistService).revokeUser("+919876543210");
        verify(refreshTokenService).revokeUserSessions("+919876543210");
    }
}
