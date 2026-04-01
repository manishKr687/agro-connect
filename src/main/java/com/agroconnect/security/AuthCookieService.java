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

    public static final String ACCESS_COOKIE_NAME = "agroconnect_auth";
    public static final String REFRESH_COOKIE_NAME = "agroconnect_refresh";

    @Value("${app.jwt.expiration-ms:1800000}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public String buildAuthCookie(String token, boolean secure) {
        return buildCookie(ACCESS_COOKIE_NAME, token, secure, accessExpirationMs / 1000);
    }

    public String buildRefreshCookie(String token, boolean secure) {
        return buildCookie(REFRESH_COOKIE_NAME, token, secure, refreshExpirationMs / 1000);
    }

    public String buildClearedAuthCookie(boolean secure) {
        return buildCookie(ACCESS_COOKIE_NAME, "", secure, 0);
    }

    public String buildClearedRefreshCookie(boolean secure) {
        return buildCookie(REFRESH_COOKIE_NAME, "", secure, 0);
    }

    private String buildCookie(String name, String value, boolean secure, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    public Optional<String> extractToken(HttpServletRequest request) {
        return extractCookieValue(request, ACCESS_COOKIE_NAME);
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_COOKIE_NAME);
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public void addAuthCookie(HttpHeaders headers, String token, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, buildAuthCookie(token, secure));
    }

    public void addRefreshCookie(HttpHeaders headers, String token, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, buildRefreshCookie(token, secure));
    }

    public void clearAuthCookie(HttpHeaders headers, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, buildClearedAuthCookie(secure));
    }

    public void clearRefreshCookie(HttpHeaders headers, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, buildClearedRefreshCookie(secure));
    }
}
