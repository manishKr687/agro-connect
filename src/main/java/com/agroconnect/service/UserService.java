package com.agroconnect.service;

import com.agroconnect.dto.LoginRequest;
import com.agroconnect.dto.RegisterUserRequest;
import com.agroconnect.dto.UpdateUserRequest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    private static final Set<Role> SELF_REGISTERABLE_ROLES = Set.of(Role.FARMER, Role.RETAILER);

    public User register(RegisterUserRequest request) {
        if (!SELF_REGISTERABLE_ROLES.contains(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role " + request.getRole() + " cannot self-register");
        }

        userRepository.findByUsername(request.getUsername()).ifPresent(existingUser -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        });

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        return userRepository.save(user);
    }

    public User createUserForAdmin(Long adminId, RegisterUserRequest request) {
        accessControlService.requireAdmin(adminId);

        userRepository.findByUsername(request.getUsername()).ifPresent(existingUser -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        });

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

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

        userRepository.findByUsername(request.getUsername()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
        });

        user.setUsername(request.getUsername().trim());
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
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

        userRepository.delete(user);
    }
}
