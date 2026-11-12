package com.opencirc.api.passport.service;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.auth.service.AdminDetailsService;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.AdminRepository;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.Admin;
import com.opencirc.api.passport.util.EncryptionUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Service to handle JWT related functions. */
@Service
public class JwtService {

  private final AdminRepository adminRepository;

  private final AppProperties appProperties;

  private final AdminDetailsService adminDetailsService;

  private String secretKey = null;

  /** Instantiating JwtService class. */
  @Autowired
  public JwtService(
      AppProperties appProperties,
      AdminRepository adminRepository,
      AdminDetailsService adminDetailsService) {
    this.appProperties = appProperties;
    this.adminRepository = adminRepository;
    this.adminDetailsService = adminDetailsService;
  }

  /** Loading Secret Key after initialization. */
  @PostConstruct
  public void init() {
    loadSecretKeyIfNeeded();
  }

  /** Loading Secret Key. */
  private void loadSecretKeyIfNeeded() {
    if (secretKey != null && !secretKey.isEmpty()) {
      return;
    }

    try {
      this.secretKey =
          EncryptionUtil.decrypt(
              appProperties.getJwtSecretKey(), appProperties.getJwtEncryptionKey());
    } catch (Exception e) {
      throw new RuntimeException("Error decrypting secret key", e);
    }
  }

  /** Generates token. */
  public String generateToken(String userId, int expiryMinutes) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    return Jwts.builder()
        .claims(claims)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(expiryMinutes))))
        .signWith(getKey())
        .compact();
  }

  private SecretKey getKey() {
    loadSecretKeyIfNeeded();
    try {
      byte[] keyBytes = Decoders.BASE64.decode(secretKey.trim());
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid Base64 secret key", e);
    }
  }

  /** Extracts the user ID. */
  public String extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", String.class));
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
  }

  /** Validates the token. */
  public boolean validateToken(String token, UserDetails userDetails) {
    if (!(userDetails instanceof AdminPrincipal adminPrincipal)) {
      return false;
    }

    final String userIdFromToken = extractUserId(token);

    return (userIdFromToken.equals(adminPrincipal.getAdminId()) && !isTokenExpired(token));
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  /** Validates the refresh token. If it is valid, generates a new access token. */
  public String generateAccessTokenUsingRefreshToken(String refreshToken) {
    String userId = extractUserId(refreshToken);
    UserDetails userDetails = adminDetailsService.loadUserById(userId);
    Admin admin =
        adminRepository
            .findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
    if (userDetails == null || !validateToken(refreshToken, userDetails)) {
      throw new AuthenticationException("Invalid refresh token");
    }

    if (!refreshToken.equals(admin.getRefreshToken())) {
      throw new AuthenticationException("Invalid Refresh Token");
    }

    return generateToken(userId, appProperties.getAccessTokenExpiryTime());
  }

  /** Sets a cookie with generated token. */
  public void generateTokenCookie(
      HttpServletResponse response, String token, String tokenType, int expiryTime) {
    ResponseCookie cookie =
        ResponseCookie.from(tokenType, token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(expiryTime)
            .sameSite("Lax")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
