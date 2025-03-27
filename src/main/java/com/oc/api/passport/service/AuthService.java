package com.oc.api.passport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oc.api.passport.config.Properties;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.dao.UserRepository;
import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.exception.AuthenticationException;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {

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
     * Instantiating BCryptPasswordEncoder class.
     */
    private BCryptPasswordEncoder bCryptPasswordEncoder =
            new BCryptPasswordEncoder(AppConstants.NUM_TWELVE);

    /**
     * Injecting AuthenticationManager class.
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Injecting JwtService class.
     */
    @Autowired
    private JwtService jwtService;

    /**
     * Injecting AuthUserDetailsService class.
     */
    @Autowired
    private AuthUserDetailsService authUserDetailsService;

    /**
     * Register new user.
     *
     * @param user details with username, email, password
     */
    public void register(UserEntity user) throws AuthenticationException {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new AuthenticationException(AppConstants.ERR_USERNAME_EXISTS);
        }

        String encodedPassword = bCryptPasswordEncoder
                .encode(user.getPassword());
        user.setPassword(encodedPassword);

        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new AuthenticationException("Error during user registration: "
        + e.getMessage());
        }
    }

    /**
     * Login and verifies the user.
     *
     * @param user details with username, password
     * @param response
     */
    public void verify(UserEntity user, HttpServletResponse response)
            throws AuthenticationException {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(),
                            user.getPassword()));
            if (!authentication.isAuthenticated()) {
                throw new AuthenticationException(
                        AppConstants.ERR_INVALID_CREDENTIALS);
            }
            String accessToken = jwtService.generateToken(user.getUsername(),
                    properties.getAccessTokenExpiryTime());
            String refreshToken = jwtService.generateToken(user.getUsername(),
                    properties.getRefreshTokenExpiryTime());

            UserEntity existingUser = userRepository.findByUsername(user.getUsername());
            existingUser.setRefreshToken(refreshToken);
            userRepository.save(existingUser);

            ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(properties.getAccessTokenExpiryTime())
                    .sameSite("Lax")
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token",
                    refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(properties.getRefreshTokenExpiryTime())  // 7 days
                    .sameSite("Lax")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        } catch (BadCredentialsException bce) {
            throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
        } catch (Exception e) {
            throw new AuthenticationException("Error during login: " + e.getMessage());
        }

    }

    /**
     * Refreshes the expired token.
     *
     * @param refreshToken - Existing JWT refresh token
     * @param response
     * @return JWT token (new access token)
     */
    public String refreshToken(String refreshToken, HttpServletResponse response)
            throws AuthenticationException {
        try {
            String newAccessToken = jwtService
                    .generateAccessTokenUsingRefreshToken(refreshToken);
            ResponseCookie accessCookie = ResponseCookie
                    .from("access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/").maxAge(properties.getAccessTokenExpiryTime())
                    .sameSite("Lax").build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            return newAccessToken;
        } catch (Exception e) {
            throw new AuthenticationException(
                    "Error refreshing token: " + e.getMessage());
        }
    }

    /**
     * Validates the token.
     *
     * @param token - JWT token
     * @return result whether the token is valid or not
     */
    public boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = authUserDetailsService.loadUserByUsername(username);

            return jwtService.validateToken(token, userDetails);
        } catch (Exception e) {
            throw new AuthenticationException("Error validating token: "
        + e.getMessage());
        }
    }

    /**
     * Logs out the user.
     *
     * @param refreshToken - JWT refresh token
     * @param response
     */
    public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        String username = jwtService.extractUsername(refreshToken);
        UserEntity existingUser = userRepository.findByUsername(username);
        existingUser.setRefreshToken(null);
        userRepository.save(existingUser);

        // Remove the JWT cookies (access_token, refresh_token)
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

    }
}
