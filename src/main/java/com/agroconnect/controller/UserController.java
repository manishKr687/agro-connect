package com.agroconnect.controller;

import com.agroconnect.dto.ChangePasswordRequest;
import com.agroconnect.model.User;
import com.agroconnect.security.AuthCookieService;
import com.agroconnect.service.AccessControlService;
import com.agroconnect.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AccessControlService accessControlService;
    private final UserService userService;
    private final AuthCookieService authCookieService;

    public UserController(AccessControlService accessControlService,
                          UserService userService,
                          AuthCookieService authCookieService) {
        this.accessControlService = accessControlService;
        this.userService = userService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/me")
    public User me() {
        return accessControlService.getCurrentUser();
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               HttpServletRequest httpRequest) {
        userService.changePassword(accessControlService.getCurrentUser(), request);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.clearAuthCookie(headers, httpRequest.isSecure());
        authCookieService.clearRefreshCookie(headers, httpRequest.isSecure());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).headers(headers).build();
    }
}
