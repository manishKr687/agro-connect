package com.agroconnect.controller;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.BlacklistedTokenRepository;
import com.agroconnect.repository.LoginAttemptRecordRepository;
import com.agroconnect.repository.PasswordResetChallengeRepository;
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

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BlacklistedTokenRepository blacklistedTokenRepository;
    @Autowired private RevokedUserRepository revokedUserRepository;
    @Autowired private LoginAttemptRecordRepository loginAttemptRecordRepository;
    @Autowired private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Autowired private PasswordResetChallengeRepository passwordResetChallengeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.jwt.issuer}") private String jwtIssuer;

    private static final String ADMIN_PHONE = "+919999999999";
    private static final String FARMER_PHONE = "+918888888888";

    @BeforeEach
    void setUp() {
        blacklistedTokenRepository.deleteAll();
        revokedUserRepository.deleteAll();
        loginAttemptRecordRepository.deleteAll();
        refreshTokenSessionRepository.deleteAll();
        passwordResetChallengeRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("Admin User")
                .phoneNumber(ADMIN_PHONE)
                .email("admin@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .name("Farmer User")
                .phoneNumber(FARMER_PHONE)
                .email("farmer@example.com")
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
        MockCookie farmerCookie = loginAndExtractCookies(FARMER_PHONE, "Password123").authCookie();

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
                .subject(ADMIN_PHONE)
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
        AuthCookies cookies = loginAndExtractCookies(ADMIN_PHONE, "Password123");
        MockCookie authCookie = cookies.authCookie();

        mockMvc.perform(get("/api/users/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin User"))
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
        AuthCookies cookies = loginAndExtractCookies(ADMIN_PHONE, "Password123");

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
                .andExpect(jsonPath("$.name").value("Admin User"));

        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordShouldClearCookiesAndRequireNewLogin() throws Exception {
        AuthCookies cookies = loginAndExtractCookies(ADMIN_PHONE, "Password123");

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

        loginAndExtractCookies(ADMIN_PHONE, "NewPassword123");
    }

    @Test
    void forgotPasswordWithEmailShouldResetPasswordAndRevokeOldSessions() throws Exception {
        AuthCookies cookies = loginAndExtractCookies(ADMIN_PHONE, "Password123");

        MvcResult forgotResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "EMAIL",
                                  "identifier": "admin@example.com"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.previewToken").isNotEmpty())
                .andReturn();

        String previewToken = JsonTestHelper.readString(forgotResult.getResponse().getContentAsString(), "previewToken");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "EMAIL",
                                  "identifier": "admin@example.com",
                                  "token": "%s",
                                  "newPassword": "RecoveredPassword123"
                                }
                                """.formatted(previewToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").cookie(cookies.authCookie()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshCookie()))
                .andExpect(status().isUnauthorized());

        loginAndExtractCookies(ADMIN_PHONE, "RecoveredPassword123");
    }

    @Test
    void forgotPasswordWithSmsShouldResetPassword() throws Exception {
        MvcResult forgotResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "SMS",
                                  "identifier": "%s"
                                }
                                """.formatted(FARMER_PHONE)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.previewOtp").isNotEmpty())
                .andReturn();

        String previewOtp = JsonTestHelper.readString(forgotResult.getResponse().getContentAsString(), "previewOtp");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "SMS",
                                  "identifier": "%s",
                                  "otp": "%s",
                                  "newPassword": "RecoveredSms123"
                                }
                                """.formatted(FARMER_PHONE, previewOtp)))
                .andExpect(status().isNoContent());

        loginAndExtractCookies(FARMER_PHONE, "RecoveredSms123");
    }

    private AuthCookies loginAndExtractCookies(String phoneNumber, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "%s",
                                  "password": "%s"
                                }
                                """.formatted(phoneNumber, password)))
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

    private record AuthCookies(MockCookie authCookie, MockCookie refreshCookie) {}

    private static final class JsonTestHelper {
        private static String readString(String json, String fieldName) {
            String token = "\"%s\":\"".formatted(fieldName);
            int start = json.indexOf(token);
            assertThat(start).isGreaterThanOrEqualTo(0);
            int valueStart = start + token.length();
            int valueEnd = json.indexOf('"', valueStart);
            assertThat(valueEnd).isGreaterThan(valueStart);
            return json.substring(valueStart, valueEnd);
        }
    }
}
