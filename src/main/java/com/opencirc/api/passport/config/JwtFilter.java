package com.opencirc.api.passport.config;

import com.opencirc.api.passport.auth.principal.ApiKeyAuthenticationToken;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.auth.service.AdminDetailsService;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Authenticates requests. A machine token ({@code Authorization: Bearer opc_live_...} or {@code
 * X-Api-Key}) is resolved with one indexed lookup and a constant-time hash comparison; otherwise
 * the administrator JWT cookies are validated.
 */
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

  /** Injecting JwtService class. */
  private final JwtService jwtService;

  /** Injecting Properties class. */
  private final AppProperties properties;

  /** Injecting ApiKeyService class. */
  private final ApiKeyService apiKeyService;

  /** Injecting AdminDetailsService class. */
  private final AdminDetailsService adminDetailsService;

  /** Injecting ObjectMapper class. */
  private final ObjectMapper objectMapper;

  /** Constructor to initialize JwtFilter dependencies. */
  public JwtFilter(
      JwtService jwtService,
      AppProperties properties,
      ApiKeyService apiKeyService,
      AdminDetailsService adminDetailsService,
      ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.properties = properties;
    this.apiKeyService = apiKeyService;
    this.adminDetailsService = adminDetailsService;
    this.objectMapper = objectMapper;
  }

  /** Processes incoming HTTP requests and validates authentication. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String apiToken = ApiKeyService.resolveToken(request);
    if (apiToken != null) {
      if (authenticateApiKey(request, response, apiToken)) {
        filterChain.doFilter(request, response);
      }
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

    String adminId = extractUserIdFromToken(accessToken, response);
    if (adminId == null) {
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      authenticateAdmin(request, response, accessToken, adminId);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Authenticates a machine token. Returns true when the security context was populated; writes the
   * error response and returns false otherwise.
   */
  private boolean authenticateApiKey(
      HttpServletRequest request, HttpServletResponse response, String token) throws IOException {
    ApiKeyPrincipal principal;
    try {
      principal = apiKeyService.authenticate(token);
    } catch (AuthenticationException e) {
      log.warn("API-key auth rejected: {}", e.getMessage());
      sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("API-key auth error", e);
      sendErrorResponse(
          response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
      return false;
    }

    ApiKeyAuthenticationToken authToken = new ApiKeyAuthenticationToken(principal);
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authToken);
    return true;
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

  /** Extracts the admin ID from the token. */
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

  /** Authenticates the administrator using the extracted ID. */
  private void authenticateAdmin(
      HttpServletRequest request, HttpServletResponse response, String accessToken, String adminId)
      throws IOException {
    try {

      UserDetails userDetails = adminDetailsService.loadUserById(adminId);

      if (!jwtService.validateToken(accessToken, userDetails)) {
        throw new AuthenticationException(AppConstants.ERR_INVALID_TOKEN);
      }

      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    } catch (UsernameNotFoundException e) {
      log.warn("JWT auth: admin not found for id {}", adminId);
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
