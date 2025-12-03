package com.opencirc.api.passport.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.JwtService;
import com.opencirc.api.passport.service.PasswordService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Filter to validate the JWT token. */
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

  /** Injecting JwtService class. */
  private final JwtService jwtService;

  /** Injecting Properties class. */
  private final AppProperties properties;

  /** Injecting ApiKeyService class. */
  private final ApiKeyService apiKeyService;

  /** Injecting PasswordService class. */
  private final PasswordService passwordService;

  /** Injecting AuthUserDetailsService class. */
  private final AuthUserDetailsService authUserDetailsService;

  /** Injecting ObjectMapper class. */
  private final ObjectMapper objectMapper;

  /** Constructor to initialize JwtFilter dependencies. */
  public JwtFilter(
      JwtService jwtService,
      AppProperties properties,
      ApiKeyService apiKeyService,
      PasswordService passwordService,
      AuthUserDetailsService authUserDetailsService,
      ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.properties = properties;
    this.apiKeyService = apiKeyService;
    this.passwordService = passwordService;
    this.authUserDetailsService = authUserDetailsService;
    this.objectMapper = objectMapper;
  }

  /** Processes incoming HTTP requests and validates authentication. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String apiKeyHeader = request.getHeader(AppConstants.HEADER_API_KEY);
    if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
      handleApiKeyAuth(request, response, filterChain, apiKeyHeader.trim());
      return;
    }

    String accessToken = extractTokenFromCookies(request, AppConstants.COOKIE_ACCESS_TOKEN);
    String refreshToken = extractTokenFromCookies(request, AppConstants.COOKIE_REFRESH_TOKEN);

    if (accessToken == null && refreshToken != null) {
      accessToken = handleRefreshToken(response, refreshToken);
      if (accessToken == null) {
        sendErrorResponse(
            response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_TOKEN);
        return;
      }
    }

    if (accessToken == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String userId = extractUserIdFromToken(accessToken, response);
    if (userId == null) {
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      authenticateUser(request, response, accessToken, userId);
    }

    filterChain.doFilter(request, response);
  }

  /** Handles authentication using API key and secret headers. */
  private void handleApiKeyAuth(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain,
      String apiKeyHeader)
      throws IOException, ServletException {

    if (apiKeyHeader == null) {
      sendErrorResponse(
          response, HttpServletResponse.SC_BAD_REQUEST, AppConstants.ERR_INVALID_CREDENTIALS);
      return;
    }

    ApiKey apiKey = apiKeyService.findById(apiKeyHeader);

    if (apiKey == null) {
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_CREDENTIALS);
      return;
    }

    String apiSecretHeader = request.getHeader(AppConstants.HEADER_API_SECRET);
    if (apiSecretHeader == null || apiSecretHeader.isBlank()) {
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_CREDENTIALS);
      return;
    }

    if (!passwordService.verifyPassword(apiSecretHeader.trim(), apiKey.getSecret())) {
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_CREDENTIALS);
      return;
    }

    if (apiKey.getExpirationTime() != null
        && !Instant.now().isBefore(apiKey.getExpirationTime().toInstant())) {
      sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "API key expired");
      return;
    }

    try {
      if (apiKey.getUserId() == null) {
        log.warn("API-key auth: missing userId on ApiKey {}", apiKey.getId());
        sendErrorResponse(
            response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_CREDENTIALS);
        return;
      }
      UserDetails userDetails = authUserDetailsService.loadUserById(apiKey.getUserId());

      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);

      filterChain.doFilter(request, response);
    } catch (UsernameNotFoundException e) {
      log.warn("API-key auth: user not found for provided API key");
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_CREDENTIALS);
    } catch (Exception e) {
      log.error("API-key auth error", e);
      sendErrorResponse(
          response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
    }
  }

  /** Extracts the token from cookies. */
  private String extractTokenFromCookies(HttpServletRequest request, String tokenName) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (tokenName.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  /** Handles refresh token logic and generates a new access token. */
  private String handleRefreshToken(HttpServletResponse response, String refreshToken) {
    try {
      String newAccessToken = jwtService.generateAccessTokenUsingRefreshToken(refreshToken);
      jwtService.generateTokenCookie(
          response,
          newAccessToken,
          AppConstants.COOKIE_ACCESS_TOKEN,
          properties.getAccessTokenExpiryTime());
      return newAccessToken;
    } catch (AuthenticationException e) {
      log.info("Refresh token rejected: {}", e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("Refresh token processing error", e);
      return null;
    }
  }

  /** Extracts the user ID from the token. */
  private String extractUserIdFromToken(String token, HttpServletResponse response)
      throws IOException {
    try {
      return jwtService.extractUserId(token);
    } catch (Exception e) {
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_TOKEN);
      return null;
    }
  }

  /** Authenticates the user using extracted user ID. */
  private void authenticateUser(
      HttpServletRequest request, HttpServletResponse response, String accessToken, String userId)
      throws IOException {
    try {

      UserDetails userDetails = authUserDetailsService.loadUserById(userId);

      if (!jwtService.validateToken(accessToken, userDetails)) {
        throw new AuthenticationException(AppConstants.ERR_INVALID_TOKEN);
      }

      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    } catch (UsernameNotFoundException e) {
      log.warn("JWT auth: user not found for id {}", userId);
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_TOKEN);
    } catch (AuthenticationException e) {
      log.warn("JWT auth failed: {}", e.getMessage());
      sendErrorResponse(
          response, HttpServletResponse.SC_UNAUTHORIZED, AppConstants.ERR_INVALID_TOKEN);
    } catch (Exception e) {
      log.error("JWT auth unexpected error", e);
      sendErrorResponse(
          response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
    }
  }

  /** Sends an error response. */
  private void sendErrorResponse(HttpServletResponse response, int status, String message)
      throws IOException {
    SecurityContextHolder.clearContext();
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    if (status == HttpServletResponse.SC_UNAUTHORIZED
        || status == HttpServletResponse.SC_FORBIDDEN) {
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("Pragma", "no-cache");
    }
    objectMapper.writeValue(response.getWriter(), Map.of("error", message));
  }
}
