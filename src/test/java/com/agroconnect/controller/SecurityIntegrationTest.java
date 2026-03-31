package com.agroconnect.controller;

import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.BlacklistedTokenRepository;
import com.agroconnect.repository.LoginAttemptRecordRepository;
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
    private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        blacklistedTokenRepository.deleteAll();
        revokedUserRepository.deleteAll();
        loginAttemptRecordRepository.deleteAll();
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
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongRoleShouldFail() throws Exception {
        MockCookie farmerCookie = loginAndExtractAuthCookie("farmer_user", "Password123");

        mockMvc.perform(get("/api/admins/1/users").cookie(farmerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidTokenShouldFail() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .cookie(new MockCookie("agroconnect_auth", "not-a-valid-jwt")))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredTokenShouldFail() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("admin_user")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/users/me")
                        .cookie(new MockCookie("agroconnect_auth", expiredToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginAndLogoutShouldWork() throws Exception {
        MockCookie authCookie = loginAndExtractAuthCookie("admin_user", "Password123");

        mockMvc.perform(get("/api/users/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin_user"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout").cookie(authCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("agroconnect_auth", 0))
                .andReturn();

        String clearedCookieHeader = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(clearedCookieHeader).contains("HttpOnly");

        mockMvc.perform(get("/api/users/me").cookie(authCookie))
                .andExpect(status().isForbidden());
    }

    private MockCookie loginAndExtractAuthCookie(String username, String password) throws Exception {
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
                .andReturn();

        String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly");

        return new MockCookie("agroconnect_auth", loginResult.getResponse().getCookie("agroconnect_auth").getValue());
    }
}
