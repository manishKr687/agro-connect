package com.agroconnect.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class AuthCookieService {

    public static final String COOKIE_NAME = "agroconnect_auth";

    @Value("${app.jwt.expiration-ms:1800000}")
    private long expirationMs;

    public String buildAuthCookie(String token, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(expirationMs / 1000)
                .build()
                .toString();
    }

    public String buildClearedCookie(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }

    public Optional<String> extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public void addAuthCookie(HttpHeaders headers, String token, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, buildAuthCookie(token, secure));
    }

    public void clearAuthCookie(HttpHeaders headers, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, buildClearedCookie(secure));
    }
}
