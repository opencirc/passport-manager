package com.oc.api.passport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oc.api.passport.config.AppProperties;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.dao.UserRepository;
import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.exception.AuthenticationException;
import com.oc.api.passport.model.UserPrincipal;

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
    private AppProperties properties;

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
            if (user.getUsername() == null || (user.getPassword() == null)) {
                throw new AuthenticationException("Not authenticated."
                        + " Username or Password is null");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(),
                            user.getPassword()));
            if (!authentication.isAuthenticated()) {
                throw new AuthenticationException(
                        AppConstants.ERR_INVALID_CREDENTIALS);
            }
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getUserId(); 
            
            String accessToken = jwtService.generateToken(userId,
                    properties.getAccessTokenExpiryTime());
            String refreshToken = jwtService.generateToken(userId,
                    properties.getRefreshTokenExpiryTime());

            userRepository.updateRefreshTokenByUserId(userId, refreshToken);
            jwtService.generateTokenCookie(response, accessToken,
                    AppConstants.COOKIE_ACCESS_TOKEN,
                    properties.getAccessTokenExpiryTime());
            jwtService.generateTokenCookie(response, refreshToken,
                    AppConstants.COOKIE_REFRESH_TOKEN,
                    properties.getRefreshTokenExpiryTime());
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
            jwtService.generateTokenCookie(response, newAccessToken,
                    AppConstants.COOKIE_ACCESS_TOKEN,
                    properties.getAccessTokenExpiryTime());
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
            Long userId = jwtService.extractUserId(token);
            UserDetails userDetails = authUserDetailsService.loadUserById(userId);

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
    public void logout(String refreshToken, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        Long userId = jwtService.extractUserId(refreshToken);
        UserEntity existingUser = userRepository.findByUserId(userId);
        existingUser.setRefreshToken(null);
        userRepository.save(existingUser);

        // Remove the JWT cookies (access_token, refresh_token)
        jwtService.generateTokenCookie(response, "",
                AppConstants.COOKIE_ACCESS_TOKEN, 0);
        jwtService.generateTokenCookie(response, "",
                AppConstants.COOKIE_REFRESH_TOKEN, 0);
    }
}
