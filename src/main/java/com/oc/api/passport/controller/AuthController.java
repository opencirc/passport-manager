package com.oc.api.passport.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.exception.AuthenticationException;
import com.oc.api.passport.service.AuthService;

/**
 * Endpoint controller for authentication.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Injecting AuthService class.
     */
    @Autowired
    private AuthService authService;

    /**
     * Endpoint to register new user.
     *
     * @param user details with username, email, password
     * @return response
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserEntity user)
            throws AuthenticationException {
        authService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }

    /**
     * Endpoint to Login.
     *
     * @param user details with username, password
     * @return response with JWT token (access and refresh tokens)
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody UserEntity user) throws AuthenticationException {
        Map<String, String> response = authService.verify(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to refresh expired token.
     *
     * @param token - Existing JWT refresh token
     * @return response with JWT token (new access token)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestParam String token) throws AuthenticationException {
        Map<String, String> response = authService.refreshToken(token);
        return ResponseEntity.ok(response);

    }
}
