package com.opencirc.api.passport.auth.controller;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.dto.LoginRequestDto;
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

/** Endpoint controller for authentication. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  /** Injecting AuthService class. */
  @Autowired private AuthService authService;

  /**
   * Endpoint to Login.
   *
   * @param loginRequest details with email, password
   * @param response
   * @return response with JWT token (access and refresh tokens)
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
   *
   * @param request
   * @return response
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

    boolean isAuthenticated =
        (token != null && authService.validateToken(token))
            || authService.validateApiKeySecret(request);

    if (!isAuthenticated) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new StatusResponseDto("Not authenticated"));
    }

    return ResponseEntity.ok(new StatusResponseDto("Authenticated"));
  }

  /**
   * Returns the currently authenticated user.
   *
   * @param request HTTP servlet request
   * @return UserDto if authenticated, or null
   */
  @GetMapping("/currentUser")
  public ResponseEntity<UserDto> getCurrentUser(HttpServletRequest request) {
    UserDto userDto = authService.getCurrentUser(request);
    return ResponseEntity.ok(userDto);
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
   * Endpoint to Logout.
   *
   * @param request
   * @param response
   * @return response with JWT token (new access token)
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException {
    try {
      Cookie[] cookies = request.getCookies();
      String token = null;

      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("refresh_token".equals(cookie.getName())) {
            token = cookie.getValue();
            break;
          }
        }
      }
      authService.logout(token, response);
      return ResponseEntity.ok(new StatusResponseDto("Logged out"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new StatusResponseDto("Error while logging out " + e.getMessage()));
    }
  }
}
