package com.oc.api.passport.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.exception.AuthenticationException;
import com.oc.api.passport.model.RegisterRequest;
import com.oc.api.passport.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;

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
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest userDetails) 
            throws AuthenticationException {
        authService.register(userDetails);
        return ResponseEntity.ok("User registered successfully");
    }

    /**
     * Endpoint to Login.
     *
     * @param user details with username, password
     * @param response
     * @return response with JWT token (access and refresh tokens)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserEntity user, HttpServletResponse response)
                    throws AuthenticationException {
        authService.verify(user, response);
        return ResponseEntity.ok(Collections.singletonMap("message", "Logged in"));
    }

    /**
     * Verifies the status of authentication.
     *
     * @param token - JWT access token
     * @param response
     * @return response
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAuth(@CookieValue(name = "access_token",
    required = false) String token, HttpServletResponse response) {
        if (token == null || !authService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Not authenticated");
        }

        return ResponseEntity.ok(Collections.singletonMap("message", "Authenticated"));
    }

    /**
     * Endpoint to refresh expired token.
     *
     * @param refreshToken - Existing JWT refresh token
     * @param response
     * @return response with JWT token (new access token)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue("refresh_token") String refreshToken, HttpServletResponse
            response) throws AuthenticationException {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token missing");
        }

        try {
            String newAccessToken = authService.refreshToken(refreshToken, response);
            return ResponseEntity.ok(Collections.singletonMap("message",
                    "Token refreshed"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error refreshing token: " + e.getMessage());
        }
    }

    /**
     * Endpoint to Logout.
     *
     * @param refreshToken - Existing JWT refresh token
     * @param response
     * @return response with JWT token (new access token)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) throws AuthenticationException {
        try {
            authService.logout(refreshToken, response);
            return ResponseEntity.ok(Collections.singletonMap("message", "Logged out"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Error while logging out " + e.getMessage());
        }

    }


}
