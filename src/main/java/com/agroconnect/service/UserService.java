package com.agroconnect.service;

import com.agroconnect.dto.LoginRequest;
import com.agroconnect.dto.RegisterUserRequest;
import com.agroconnect.dto.UpdateUserRequest;
import com.agroconnect.dto.ChangePasswordRequest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import com.agroconnect.security.LoginAttemptService;
import com.agroconnect.security.RefreshTokenService;
import com.agroconnect.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Handles user registration, authentication, and admin-level user management.
 *
 * <p>Self-registration is restricted to {@code FARMER} and {@code RETAILER} roles.
 * {@code ADMIN} and {@code AGENT} accounts can only be created by an existing admin
 * via {@link #createUserForAdmin(Long, RegisterUserRequest)}.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;

    /** Roles that are allowed to self-register via the public {@code /api/auth/register} endpoint. */
    private static final Set<Role> SELF_REGISTERABLE_ROLES = Set.of(Role.FARMER, Role.RETAILER);

    /**
     * Registers a new farmer or retailer account.
     *
     * @throws org.springframework.web.server.ResponseStatusException 403 if the requested role is ADMIN or AGENT
     * @throws org.springframework.web.server.ResponseStatusException 409 if the username is already taken
     */
    public User register(RegisterUserRequest request) {
        if (!SELF_REGISTERABLE_ROLES.contains(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role " + request.getRole() + " cannot self-register");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        validateRecoveryContacts(normalizedEmail, normalizedPhoneNumber);
        assertUniqueIdentity(request.getUsername(), normalizedEmail, normalizedPhoneNumber, null);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .phoneNumber(normalizedPhoneNumber)
                .role(request.getRole())
                .build();

        return userRepository.save(user);
    }

    /**
     * Creates a user of any role on behalf of an admin. Used to provision ADMIN and AGENT accounts.
     *
     * @throws org.springframework.web.server.ResponseStatusException 403 if the caller is not an ADMIN
     * @throws org.springframework.web.server.ResponseStatusException 409 if the username is already taken
     */
    public User createUserForAdmin(Long adminId, RegisterUserRequest request) {
        accessControlService.requireAdmin(adminId);
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        validateRecoveryContacts(normalizedEmail, normalizedPhoneNumber);
        assertUniqueIdentity(request.getUsername(), normalizedEmail, normalizedPhoneNumber, null);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .phoneNumber(normalizedPhoneNumber)
                .role(request.getRole())
                .build();

        return userRepository.save(user);
    }

    /**
     * Validates credentials and returns the authenticated user.
     * The caller ({@link com.agroconnect.controller.AuthController}) then generates a JWT from the result.
     *
     * @throws org.springframework.web.server.ResponseStatusException 401 if the username doesn't exist or the password is wrong
     */
    public User login(LoginRequest request) {
        String username = request.getUsername();

        if (loginAttemptService.isLocked(username)) {
            long seconds = loginAttemptService.secondsUntilUnlock(username);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Account temporarily locked. Try again in " + seconds + " seconds.");
        }

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        loginAttemptService.recordSuccess(username);
        return user;
    }

    public List<User> getAllUsersForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return userRepository.findAll();
    }

    public User updateUserForAdmin(Long adminId, Long userId, UpdateUserRequest request) {
        accessControlService.requireAdmin(adminId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        validateRecoveryContacts(normalizedEmail, normalizedPhoneNumber);
        assertUniqueIdentity(request.getUsername(), normalizedEmail, normalizedPhoneNumber, userId);

        user.setUsername(request.getUsername().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            tokenBlacklistService.revokeUser(user.getUsername());
            refreshTokenService.revokeUserSessions(user.getUsername());
        }

        return userRepository.save(user);
    }

    public void deleteUserForAdmin(Long adminId, Long userId) {
        User admin = accessControlService.requireAdmin(adminId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (admin.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot delete their own account");
        }

        tokenBlacklistService.revokeUser(user.getUsername());
        refreshTokenService.revokeUserSessions(user.getUsername());
        userRepository.delete(user);
    }

    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        tokenBlacklistService.revokeUser(currentUser.getUsername());
        refreshTokenService.revokeUserSessions(currentUser.getUsername());
    }

    private void validateRecoveryContacts(String email, String phoneNumber) {
        if ((email == null || email.isBlank()) && (phoneNumber == null || phoneNumber.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either email or phone number is required");
        }
    }

    private void assertUniqueIdentity(String username, String email, String phoneNumber, Long currentUserId) {
        userRepository.findByUsername(username.trim()).ifPresent(existingUser -> {
            if (!Objects.equals(existingUser.getId(), currentUserId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
        });

        if (email != null) {
            userRepository.findByEmail(email).ifPresent(existingUser -> {
                if (!Objects.equals(existingUser.getId(), currentUserId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                }
            });
        }

        if (phoneNumber != null) {
            userRepository.findByPhoneNumber(phoneNumber).ifPresent(existingUser -> {
                if (!Objects.equals(existingUser.getId(), currentUserId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
                }
            });
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        return phoneNumber.trim().replaceAll("[\\s()\\-]", "");
    }
}
