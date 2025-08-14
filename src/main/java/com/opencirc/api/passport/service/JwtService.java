package com.opencirc.api.passport.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

/**
 * Service to handle JWT related funtions.
 */
@Service
public class JwtService {

    /**
     * Injecting UserRepository class.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Injecting JwtConfigRepository class.
     */
    @Autowired
    private JwtConfigRepository jwtConfigRepository;

    /**
     * Injecting Properties class.
     */
    private final AppProperties appProperties;

    /**
     * Injecting AuthUserDetailsService class.
     */
    @Autowired
    private AuthUserDetailsService authUserDetailsService;

    /**
     * Secret key declaration.
     */
    private String secretKey = "";


    /**
     * Instantiating JwtService class.
     * @param appProp
     */
    @Autowired
    public JwtService(AppProperties appProp) {
        this.appProperties = appProp;

    }

    /**
     * Loading Secret Key after initialization.
     */
    @PostConstruct
    public void init() {
        loadSecretKey();
    }

    /**
     * Loading Secret Key.
     */
    private void loadSecretKey() {

        if (secretKey == null || secretKey.isEmpty()) {
            Optional<JwtConfig> keyEntity = jwtConfigRepository.getSecretKey();
            if (keyEntity.isPresent()) {
                try {
                    this.secretKey = EncryptionUtil.decrypt(keyEntity.get()
                            .getSecretKey(), appProperties.getEncryptionKey());
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
            String encryptedKey = EncryptionUtil.encrypt(rawKey,
                    appProperties.getEncryptionKey());
            this.secretKey = rawKey;
            jwtConfigRepository.saveConfig(encryptedKey);

        } catch (Exception e) {
            throw new RuntimeException("Error generating and storing secret key", e);
        }
    }

    /**
     * Generates token.
     *
     * @param userId
     * @param expiryMinutes
     * @return token
     */
    public String generateToken(String userId, int expiryMinutes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return Jwts.builder().claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(Date.from(Instant.now().plus(Duration
                        .ofMinutes(expiryMinutes))))
                .signWith(getKey()).compact();

    }

    /**
     * Retrieves the secret key.
     *
     * @return secret key
     */
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


    /**
     * Extracts the user ID.
     * @param token JWT token
     * @return User ID
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extracts the details based on claim resolver.
     * @param token
     * @param claimResolver
     * @param <T>
     * @return claims
     */
    private <T> T extractClaim(String token,
            Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    /**
     * Extracts all the details.
     * @param token
     * @return claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    /**
     * Validates the token.
     * @param token
     * @param userDetails
     * @return status
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        if (!(userDetails instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        final String userIdFromToken = extractUserId(token);

        return (userIdFromToken.equals(userPrincipal.getUserId())
                && !isTokenExpired(token));
    }

    /**
     * Checks if the token is still valid or expired.
     * @param token
     * @return status
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());

    }

    /**
     * Retrieves the user details from jwt token.
     * @param token
     * @return user details
     */
    public User extractUserFromToken(String token) {
        Optional<User> user = userRepository.findById(UUID
                .fromString(extractUserId(extractUserId(token))));
        return user.orElseThrow(
                () -> new UsernameNotFoundException("Invalid token, user not found"));
    }

    /**
     * Validates the refresh token. If it is valid, generates new access token .
     * @param refreshToken
     * @return access Token
     */
    public String generateAccessTokenUsingRefreshToken(String refreshToken) {
        String userId = extractUserId(refreshToken);
        UserDetails userDetails = authUserDetailsService.loadUserById(userId);
        User user = userRepository.findById(UUID
                .fromString(userId)).orElseThrow(
                () -> new UsernameNotFoundException("User not found"));
        if (userDetails == null || !validateToken(refreshToken, userDetails)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AuthenticationException("Invalid Refresh Token");
        }
        String newAccessToken = generateToken(userId,
                appProperties.getAccessTokenExpiryTime());
        return newAccessToken;
    }

    /**
     * Sets a cookie with generated token.
     * @param response
     * @param token
     * @param tokenType
     * @param expiryTime

     */
    public void generateTokenCookie(HttpServletResponse response,
            String token, String tokenType, int expiryTime) {
        ResponseCookie cookie = ResponseCookie
                .from(tokenType, token)
                .httpOnly(true)
                .secure(false)
                .path("/").maxAge(expiryTime)
                .sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
