package com.agroconnect.admin;

import com.agroconnect.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;
        @Autowired
        private com.agroconnect.repository.UserRepository userRepository;

    @PostMapping("/login")
    public org.springframework.http.ResponseEntity<?> adminLogin(@RequestBody com.agroconnect.model.User user) {
        com.agroconnect.model.User dbUser = userRepository.findByPhone(user.getPhone())
            .orElse(null);
        if (dbUser == null || dbUser.getRole() != com.agroconnect.model.enums.Role.ADMIN) {
            return org.springframework.http.ResponseEntity.status(401).body("Invalid credentials");
        }
        // Use passwordEncoder.matches for hash comparison
        if (!passwordEncoder.matches(user.getPasswordHash(), dbUser.getPasswordHash())) {
            return org.springframework.http.ResponseEntity.status(401).body("Invalid credentials");
        }
        String token = "dummy-admin-token"; // Replace with real JWT
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
            "token", token,
            "role", dbUser.getRole().name(),
            "message", "Login successful",
            "adminId", dbUser.getId()
        ));
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public org.springframework.http.ResponseEntity<?> adminRegister(@RequestBody com.agroconnect.model.User user) {
        if (user.getName() == null || user.getPhone() == null || user.getPasswordHash() == null || user.getName().isEmpty() || user.getPhone().isEmpty() || user.getPasswordHash().isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body("All fields are required");
        }
        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            return org.springframework.http.ResponseEntity.badRequest().body("Phone already registered");
        }
        user.setRole(com.agroconnect.model.enums.Role.ADMIN);
        user.setStatus(com.agroconnect.model.enums.Status.APPROVED);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        userRepository.save(user);
        return org.springframework.http.ResponseEntity.ok("Admin registered successfully");
    }

    @PostMapping("/logout")
    public org.springframework.http.ResponseEntity<?> adminLogout() {
        // For stateless JWT, logout is handled on frontend by clearing token.
        // Optionally, implement token blacklist if needed.
        return org.springframework.http.ResponseEntity.ok("Logout successful");
    }
}
