package com.opencirc.api.passport.service;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.JwtConfigRepository;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.JwtConfig;
import com.opencirc.api.passport.model.User;
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
import java.util.Optional;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Service to handle JWT related funtions. */
@Service
public class JwtService {

  private final UserRepository userRepository;

  private final JwtConfigRepository jwtConfigRepository;

  private final AppProperties appProperties;

  private final AuthUserDetailsService authUserDetailsService;

  private String secretKey = "";

  /** Instantiating JwtService class. */
  @Autowired
  public JwtService(AppProperties appProp, UserRepository userRepository, JwtConfigRepository jwtConfigRepository, AuthUserDetailsService authUserDetailsService) {
    this.appProperties = appProp;
    this.userRepository = userRepository;
    this.jwtConfigRepository = jwtConfigRepository;
    this.authUserDetailsService = authUserDetailsService;
  }

  /** Loading Secret Key after initialization. */
  @PostConstruct
  public void init() {
    loadSecretKey();
  }

  /** Loading Secret Key. */
  private void loadSecretKey() {

    if (secretKey == null || secretKey.isEmpty()) {
      Optional<JwtConfig> keyEntity = jwtConfigRepository.getSecretKey();
      if (keyEntity.isPresent()) {
        try {
          this.secretKey =
              EncryptionUtil.decrypt(
                  keyEntity.get().getSecretKey(), appProperties.getEncryptionKey());
        } catch (Exception e) {
          throw new RuntimeException("Error decrypting secret key", e);
        }
      } else {
        generateAndStoreSecretKey();
      }
    }
  }

  private void generateAndStoreSecretKey() {
    try {
      String rawKey = EncryptionUtil.generateSecureKey();
      String encryptedKey = EncryptionUtil.encrypt(rawKey, appProperties.getEncryptionKey());
      this.secretKey = rawKey;
      jwtConfigRepository.saveConfig(encryptedKey);

    } catch (Exception e) {
      throw new RuntimeException("Error generating and storing secret key", e);
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
    if (secretKey == null || secretKey.isEmpty()) {
      throw new RuntimeException("Secret key is missing or invalid");
    }
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
    if (!(userDetails instanceof UserPrincipal userPrincipal)) {
      return false;
    }

    final String userIdFromToken = extractUserId(token);

    return (userIdFromToken.equals(userPrincipal.getUserId()) && !isTokenExpired(token));
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  /** Validates the refresh token. If it is valid, generates a new access token. */
  public String generateAccessTokenUsingRefreshToken(String refreshToken) {
    String userId = extractUserId(refreshToken);
    UserDetails userDetails = authUserDetailsService.loadUserById(userId);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    if (userDetails == null || !validateToken(refreshToken, userDetails)) {
      throw new AuthenticationException("Invalid refresh token");
    }

    if (!refreshToken.equals(user.getRefreshToken())) {
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
