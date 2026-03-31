package com.agroconnect.controller;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.BlacklistedTokenRepository;
import com.agroconnect.repository.LoginAttemptRecordRepository;
import com.agroconnect.repository.RefreshTokenSessionRepository;
import com.agroconnect.repository.RevokedUserRepository;
import com.agroconnect.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private RevokedUserRepository revokedUserRepository;

    @Autowired
    private LoginAttemptRecordRepository loginAttemptRecordRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    @BeforeEach
    void setUp() {
        blacklistedTokenRepository.deleteAll();
        revokedUserRepository.deleteAll();
        loginAttemptRecordRepository.deleteAll();
        refreshTokenSessionRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .username("admin_user")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .username("farmer_user")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.FARMER)
                .build());
    }

    @Test
    void unauthorizedRequestShouldFail() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleShouldFail() throws Exception {
        MockCookie farmerCookie = loginAndExtractCookies("farmer_user", "Password123").authCookie();

        mockMvc.perform(get("/api/admins/1/users").cookie(farmerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidTokenShouldFail() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .cookie(new MockCookie("agroconnect_auth", "not-a-valid-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenShouldFail() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("admin_user")
                .issuer(jwtIssuer)
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/users/me")
                        .cookie(new MockCookie("agroconnect_auth", expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndLogoutShouldWork() throws Exception {
        AuthCookies cookies = loginAndExtractCookies("admin_user", "Password123");
        MockCookie authCookie = cookies.authCookie();

        mockMvc.perform(get("/api/users/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin_user"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookies.authCookie())
                        .cookie(cookies.refreshCookie()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("agroconnect_auth", 0))
                .andExpect(cookie().maxAge("agroconnect_refresh", 0))
                .andReturn();

        assertThat(logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .allMatch(header -> header.contains("HttpOnly"));

        mockMvc.perform(get("/api/users/me").cookie(authCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshShouldRotateCookiesAndKeepSessionValid() throws Exception {
        AuthCookies cookies = loginAndExtractCookies("admin_user", "Password123");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(cookies.refreshCookie()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("agroconnect_auth"))
                .andExpect(cookie().exists("agroconnect_refresh"))
                .andReturn();

        MockCookie refreshedAuthCookie = toMockCookie(refreshResult, "agroconnect_auth");
        MockCookie refreshedRefreshCookie = toMockCookie(refreshResult, "agroconnect_refresh");

        assertThat(refreshedAuthCookie).isNotNull();
        assertThat(refreshedRefreshCookie).isNotNull();
        assertThat(refreshedRefreshCookie.getValue()).isNotEqualTo(cookies.refreshCookie().getValue());

        mockMvc.perform(get("/api/users/me").cookie(refreshedAuthCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin_user"));

        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordShouldClearCookiesAndRequireNewLogin() throws Exception {
        AuthCookies cookies = loginAndExtractCookies("admin_user", "Password123");

        mockMvc.perform(post("/api/users/me/change-password")
                        .cookie(cookies.authCookie())
                        .cookie(cookies.refreshCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Password123",
                                  "newPassword": "NewPassword123"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("agroconnect_auth", 0))
                .andExpect(cookie().maxAge("agroconnect_refresh", 0));

        mockMvc.perform(get("/api/users/me").cookie(cookies.authCookie()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshCookie()))
                .andExpect(status().isUnauthorized());

        loginAndExtractCookies("admin_user", "NewPassword123");
    }

    private AuthCookies loginAndExtractCookies(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("agroconnect_auth"))
                .andExpect(cookie().exists("agroconnect_refresh"))
                .andReturn();

        assertThat(loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .allMatch(header -> header.contains("HttpOnly"));

        return new AuthCookies(
                toMockCookie(loginResult, "agroconnect_auth"),
                toMockCookie(loginResult, "agroconnect_refresh")
        );
    }

    private MockCookie toMockCookie(MvcResult result, String name) {
        jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).isNotNull();
        return new MockCookie(name, cookie.getValue());
    }

    private record AuthCookies(MockCookie authCookie, MockCookie refreshCookie) {
    }
}
