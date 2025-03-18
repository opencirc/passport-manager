package com.oc.api.passport.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oc.api.passport.config.Properties;
import com.oc.api.passport.constants.AppConstants;
import com.oc.api.passport.dao.UserRepository;
import com.oc.api.passport.dto.UserEntity;
import com.oc.api.passport.exception.AuthenticationException;

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

        userRepository.save(user);
    }

    /**
     * Login and verifies the user.
     *
     * @param user details with username, password
     * @return response with JWT token (access and refresh tokens)
     */
    public Map<String, String> verify(UserEntity user)
            throws AuthenticationException {

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
        Map<String, String> response = new HashMap<>();
        response.put(AppConstants.ACCESS_TOKEN, accessToken);
        response.put(AppConstants.REFRESH_TOKEN, refreshToken);
        return response;

    }

    /**
     * Refreshes the expired token.
     *
     * @param token - Existing JWT refresh token
     * @return response with JWT token (new access token)
     */
    public Map<String, String> refreshToken(String token)
            throws AuthenticationException {
        String username = jwtService.extractUsername(token);
        UserDetails userDetails = authUserDetailsService
                .loadUserByUsername(username);
        if (!jwtService.validateToken(token, userDetails)) {
            throw new AuthenticationException(AppConstants.ERR_INVALID_TOKEN);
        }

        String newAccessToken = jwtService.generateToken(username,
                properties.getAccessTokenExpiryTime());
        Map<String, String> response = new HashMap<>();
        response.put(AppConstants.ACCESS_TOKEN, newAccessToken);

        return response;

    }

}
