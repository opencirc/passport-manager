package com.oc.api.passport.service;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.oc.api.passport.config.Properties;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.dao.UserRepository;
import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.exception.AuthenticationException;
import com.oc.api.passport.model.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
     * Injecting Properties class.
     */
    @Autowired
    private Properties properties;

    /**
     * Injecting AuthUserDetailsService class.
     */
    @Autowired
    private AuthUserDetailsService authUserDetailsService;

    /**
     * Secret key declaration.
     */
    private String secretkey = "";

    /**
     * Instantiating JwtService class.
     */
    public JwtService() {

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            secretkey = Base64.getEncoder()
                    .encodeToString(keyGen.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates token.
     *
     * @param userId
     * @param expiryMinutes
     * @return token
     */
    public String generateToken(Long userId, int expiryMinutes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return Jwts.builder().claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiryMinutes
                        * AppConstants.NUM_SIXTY * AppConstants.NUM_THOUSAND))
                .signWith(getKey()).compact();

    }

    /**
     * Retrieves the secret key.
     *
     * @return secret key
     */
    private SecretKey getKey() {

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretkey.trim());

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
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
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
        final Long userIdFromToken = extractUserId(token);

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
    public UserEntity extractUserFromToken(String token) {
        return userRepository.findByUserId(extractUserId(token));
    }

    /**
     * Validates the refresh token. If it is valid, generates new access token .
     * @param refreshToken
     * @return access Token
     */
    public String generateAccessTokenUsingRefreshToken(String refreshToken) {
        Long userId = extractUserId(refreshToken);
        UserDetails userDetails = authUserDetailsService.loadUserById(userId);
        UserEntity user = userRepository.findByUserId(userId);
        if (!validateToken(refreshToken, userDetails)) {
            throw new AuthenticationException(AppConstants.ERR_INVALID_TOKEN);
        }

        if (userDetails == null
                || !validateToken(refreshToken, userDetails)) {
            throw new AuthenticationException("Invalid refresh token");
        }
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AuthenticationException("Invalid Refresh Token");
        }
        String newAccessToken = generateToken(userId,
                properties.getAccessTokenExpiryTime());
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
        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", token)
                .httpOnly(true)
                .secure(false)
                .path("/").maxAge(properties.getAccessTokenExpiryTime())
                .sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }
}
