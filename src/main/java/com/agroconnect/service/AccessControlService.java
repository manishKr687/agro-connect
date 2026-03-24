package com.agroconnect.service;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Central guard for all API operations.
 *
 * <p>Every service method that requires authentication calls into this class before
 * touching any data. It reads the authenticated principal from the Spring Security
 * context and checks that the caller holds the expected identity and/or role.
 *
 * <p>All checks throw {@code 401 Unauthorized} or {@code 403 Forbidden}
 * {@link org.springframework.web.server.ResponseStatusException} on failure,
 * which Spring translates directly into HTTP error responses.
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {
    private final UserRepository userRepository;

    /**
     * Returns the currently authenticated {@link User} by resolving the username
     * stored in the Spring Security context.
     *
     * @throws org.springframework.web.server.ResponseStatusException 401 if no authentication is present
     * @throws org.springframework.web.server.ResponseStatusException 401 if the authenticated user no longer exists in the DB
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    /**
     * Verifies that the authenticated user matches {@code expectedUserId} and holds {@code requiredRole}.
     *
     * <p>Used by farmer and retailer service methods to prevent users from acting on each other's data.
     *
     * @param expectedUserId the ID that must match the authenticated user's ID
     * @param requiredRole   the role the authenticated user must have
     * @return the verified {@link User}
     * @throws org.springframework.web.server.ResponseStatusException 403 if IDs don't match or role differs
     */
    public User requireCurrentUser(Long expectedUserId, Role requiredRole) {
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(expectedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own data");
        }
        if (currentUser.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role for this action");
        }
        return currentUser;
    }

    /**
     * Verifies that the authenticated user is an ADMIN.
     *
     * <p>The {@code expectedAdminId} parameter is accepted for API symmetry with other methods
     * but is not actually checked — role alone is sufficient for admin operations.
     *
     * @return the verified admin {@link User}
     * @throws org.springframework.web.server.ResponseStatusException 403 if the caller is not an ADMIN
     */
    public User requireAdmin(Long expectedAdminId) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role for this action");
        }
        return currentUser;
    }

    public User requireAdmin() {
        return requireAdmin(null);
    }
}
