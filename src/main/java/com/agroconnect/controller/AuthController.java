package com.agroconnect.controller;

import com.agroconnect.dto.AuthResponse;
import com.agroconnect.dto.LoginRequest;
import com.agroconnect.dto.RegisterUserRequest;
import com.agroconnect.model.User;
import com.agroconnect.security.CustomUserDetails;
import com.agroconnect.security.JwtUtil;
import com.agroconnect.security.TokenBlacklistService;
import com.agroconnect.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints — no JWT required.
 *
 * <p>Both endpoints return an {@link com.agroconnect.dto.AuthResponse} containing the JWT,
 * user ID, username, and role. The frontend stores this and includes the JWT as
 * {@code Authorization: Bearer <token>} on all subsequent requests.
 *
 * <p>Rate-limited by {@link com.agroconnect.security.AuthRateLimitingFilter}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterUserRequest request) {
        User user = userService.register(request);
        return buildAuthResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request);
        return buildAuthResponse(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token, jwtUtil.extractExpiry(token));
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
