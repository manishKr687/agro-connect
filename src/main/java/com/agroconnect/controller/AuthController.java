package com.agroconnect.controller;

import com.agroconnect.model.User;
import com.agroconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.agroconnect.security.JwtUtil;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    // ...existing code...

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        User dbUser = userRepository.findByPhone(user.getPhone())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> response = new HashMap<>();
        if (passwordEncoder.matches(user.getPasswordHash(), dbUser.getPasswordHash())) {
            String token = jwtUtil.generateToken(dbUser.getPhone(), dbUser.getRole().name());
            response.put("token", token);
            response.put("role", dbUser.getRole().name());
            response.put("message", "Login successful");
        } else {
            response.put("message", "Invalid credentials");
        }
        return response;
    }
}
