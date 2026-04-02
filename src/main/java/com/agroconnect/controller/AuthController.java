package com.agroconnect.controller;

import com.agroconnect.dto.AuthResponse;
import com.agroconnect.dto.ForgotPasswordRequest;
import com.agroconnect.dto.ForgotPasswordResponse;
import com.agroconnect.dto.LoginRequest;
import com.agroconnect.dto.RegisterUserRequest;
import com.agroconnect.dto.ResetPasswordRequest;
import com.agroconnect.model.User;
import com.agroconnect.security.AuthCookieService;
import com.agroconnect.security.CustomUserDetails;
import com.agroconnect.security.InvalidRefreshTokenException;
import com.agroconnect.security.JwtUtil;
import com.agroconnect.security.PasswordResetService;
import com.agroconnect.security.RefreshTokenService;
import com.agroconnect.security.TokenBlacklistService;
import com.agroconnect.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints that issue the app JWT as an HTTP-only cookie.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthCookieService authCookieService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request,
                                                 HttpServletRequest httpRequest) {
        User user = userService.register(request);
        return buildAuthResponse(user, httpRequest, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        User user = userService.login(request);
        return buildAuthResponse(user, httpRequest, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        authCookieService.extractToken(request).ifPresent(token -> {
            try {
                tokenBlacklistService.blacklistToken(token, jwtUtil.extractExpiry(token));
            } catch (Exception ignored) {
                // Expired/invalid access tokens should not block logout cleanup.
            }
        });
        authCookieService.extractRefreshToken(request).ifPresent(refreshTokenService::revoke);
        authCookieService.clearAuthCookie(headers, request.isSecure());
        authCookieService.clearRefreshCookie(headers, request.isSecure());
        return ResponseEntity.noContent().headers(headers).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        try {
            String refreshToken = authCookieService.extractRefreshToken(request)
                    .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is missing."));

            RefreshTokenService.RotatedRefreshSession rotated = refreshTokenService.rotate(refreshToken);
            authCookieService.addAuthCookie(headers, jwtUtil.generateToken(new CustomUserDetails(rotated.user())), request.isSecure());
            authCookieService.addRefreshCookie(headers, rotated.token(), request.isSecure());
            return ResponseEntity.noContent().headers(headers).build();
        } catch (InvalidRefreshTokenException ex) {
            authCookieService.clearAuthCookie(headers, request.isSecure());
            authCookieService.clearRefreshCookie(headers, request.isSecure());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).build();
        }
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(User user, HttpServletRequest request, HttpStatus status) {
        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        RefreshTokenService.RefreshSession refreshSession = refreshTokenService.issueToken(user);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookie(headers, token, request.isSecure());
        authCookieService.addRefreshCookie(headers, refreshSession.token(), request.isSecure());

        return ResponseEntity.status(status)
                .headers(headers)
                .body(AuthResponse.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .role(user.getRole())
                        .build());
    }
}
