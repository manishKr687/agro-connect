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

@Service
@RequiredArgsConstructor
public class AccessControlService {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

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

    public User requireAdmin(Long expectedAdminId) {
        return requireCurrentUser(expectedAdminId, Role.ADMIN);
    }
}
