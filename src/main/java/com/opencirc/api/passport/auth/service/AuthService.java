package com.opencirc.api.passport.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.LoginRequestDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import com.opencirc.api.passport.service.JwtService;
import com.opencirc.api.passport.service.PasswordService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
     * Injecting PasswordService class.
     */
    @Autowired
    private PasswordService passwordService;

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
     * @param email normalized email (trimmed, lowercased)
     * @param password raw password
     * @param firstName trimmed first name
     * @param lastName  trimmed last name
     * @param role desired role (defaults to USER if null)
     * @return userId the persisted user's UUID as string
     */
    @Transactional
    public String register(String email, String password, String firstName,
            String lastName, Role role) throws AuthenticationException {

        validateRegistrationFields(email, password);

        if (userRepository.existsByEmail(email)) {
            throw new AuthenticationException("Email is already registered.");
        }

        User user = new User();

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role != null ? role : Role.USER);
        user.setActive(true);
        user.setCreatedTime(LocalDateTime.now());
        user.setPassword(passwordService.hashPassword(password));

        try {
            user = userRepository.save(user);
            return user.getId().toString();
        } catch (Exception e) {
            throw new AuthenticationException("Error during user registration.", e);
        }
    }

    /**
     * Validates registration fields (email and password).
     *
     * @param email     the user's email address
     * @param password  the user's password
     * @throws AuthenticationException if any validation rule fails
     */
    private void validateRegistrationFields(String email, String password) {
        EmailValidator emailValidator = EmailValidator.getInstance();

        if (!emailValidator.isValid(email)) {
            throw new AuthenticationException("Invalid email format.");
        }

        if (!isPasswordStrong(password)) {
            throw new AuthenticationException(
                    "Weak password. Use at least 8 characters and mix of "
                            + "upper/lowercase, digits, or symbols.");
        }

    }

    /**
     * Checks whether the given password meets the complexity criteria. The password
     * must be at least 8 characters and contain at least three of the following
     * character types:Lower case, Upper case, Digits, Special characters
     *
     * @param password
     * @return true if the password meets the criteria, false otherwise
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLower = false, hasUpper = false, hasDigit = false, hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLowerCase(ch))
                hasLower = true;
            else if (Character.isUpperCase(ch))
                hasUpper = true;
            else if (Character.isDigit(ch))
                hasDigit = true;
            else
                hasSpecial = true;
            int classes = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0)
                    + (hasSpecial ? 1 : 0);
            if (classes >= 3)
                return true;
        }
        int classes = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0)
                + (hasSpecial ? 1 : 0);
        return classes >= 3;
    }

    /**
     * Login and verifies the user.
     *
     * @param loginRequest details with username, password
     * @param response
     * @return userDto the instance of UserDto
     */
    public UserDto login(LoginRequestDto loginRequest, HttpServletResponse response)
            throws AuthenticationException {
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            throw new AuthenticationException("Username or password must not be null");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                            loginRequest.getPassword()));

            if (!authentication.isAuthenticated()) {
                throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUserId();

            String accessToken = jwtService.generateToken(userId,
                    properties.getAccessTokenExpiryTime());
            String refreshToken = jwtService.generateToken(userId,
                    properties.getRefreshTokenExpiryTime());

            userRepository.updateRefreshTokenById(UUID.fromString(userId), refreshToken);

            jwtService.generateTokenCookie(response, accessToken,
                    AppConstants.COOKIE_ACCESS_TOKEN,
                    properties.getAccessTokenExpiryTime());
            jwtService.generateTokenCookie(response, refreshToken,
                    AppConstants.COOKIE_REFRESH_TOKEN,
                    properties.getRefreshTokenExpiryTime());

            return UserDto.from(userPrincipal);

        } catch (BadCredentialsException ex) {
            throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
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
                    "Error refreshing token", e);
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
            String userId = jwtService.extractUserId(token);
            UserDetails userDetails = authUserDetailsService.loadUserById(userId);

            return jwtService.validateToken(token, userDetails);
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return false;
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
        try {
            String userId = jwtService.extractUserId(refreshToken);
            userRepository.updateRefreshTokenById(UUID.fromString(userId), null);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID during logout for token: {}", refreshToken);
        } catch (Exception e) {
            log.debug("Ignoring logout error during token extraction/update", e);
        } finally {
            // Always clear cookies
            jwtService.generateTokenCookie(response, "", AppConstants.COOKIE_ACCESS_TOKEN,
                    0);
            jwtService.generateTokenCookie(response, "",
                    AppConstants.COOKIE_REFRESH_TOKEN, 0);
        }
    }

    /**
     * Gets the details of current logged in user.
     *
     * @param request - Http servlet request
     * @return the instance of UserDto or null
     */
    public UserDto getCurrentUser(HttpServletRequest request) {

        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AppConstants.COOKIE_ACCESS_TOKEN.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null || !validateToken(token)) {
            return null;
        }

        String userId = jwtService.extractUserId(token);
        Optional<User> user = userRepository.findById(UUID.fromString(userId));
        return user.map(UserDto::from).orElse(null);

    }
}
