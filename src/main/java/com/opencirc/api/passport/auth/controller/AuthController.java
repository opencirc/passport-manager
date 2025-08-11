package com.opencirc.api.passport.auth.controller;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.dto.LoginRequestDto;
import com.opencirc.api.passport.dto.RegisterUserDto;
import com.opencirc.api.passport.dto.StatusResponseDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.exception.AuthenticationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired private AuthService authService;

  /**
   * Endpoint for registering a new user.
   */
  @PostMapping("/register")
  public ResponseEntity<?> registerUser(@RequestBody RegisterUserDto userDetails)
      throws AuthenticationException {
    authService.register(userDetails);
    return ResponseEntity.ok(new StatusResponseDto("User registered successfully"));
  }

  /**
   * Endpoint for logging a user in.
   */
  @PostMapping("/login")
  public ResponseEntity<UserDto> login(
      @RequestBody LoginRequestDto loginRequest, HttpServletResponse response)
      throws AuthenticationException {
    UserDto userDto = authService.login(loginRequest, response);
    return ResponseEntity.ok(userDto);
  }

  /**
   * Verifies the status of authentication.
   */
  @GetMapping("/status")
  public ResponseEntity<StatusResponseDto> checkAuth(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    String token = null;

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("access_token".equals(cookie.getName())) {
          token = cookie.getValue();
          break;
        }
      }
    }

    if (token == null || !authService.validateToken(token)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new StatusResponseDto("Not authenticated"));
    }

    return ResponseEntity.ok(new StatusResponseDto("Authenticated"));
  }

  /**
   * Returns the currently authenticated user.
   */
  @GetMapping("/currentUser")
  public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {
    UserDto userDto = authService.getCurrentUser(request);
    return ResponseEntity.ok(userDto);
  }

  /**
   * Endpoint for refreshing an expired JWT refresh token.
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(
      @CookieValue("refresh_token") String refreshToken, HttpServletResponse response)
      throws AuthenticationException {
    if (refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new StatusResponseDto("Refresh token missing"));
    }

    try {
      authService.refreshToken(refreshToken, response);
      return ResponseEntity.ok(new StatusResponseDto("Token refreshed"));
    } catch (AuthenticationException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new StatusResponseDto("Error refreshing token: " + e.getMessage()));
    }
  }

  /**
   * Endpoint for logging a user out.
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(
      @CookieValue(value = "refresh_token", required = false) String refreshToken,
      HttpServletResponse response)
      throws AuthenticationException {
    try {
      authService.logout(refreshToken, response);
      return ResponseEntity.ok(new StatusResponseDto("Logged out"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new StatusResponseDto("Error while logging out " + e.getMessage()));
    }
  }
}
