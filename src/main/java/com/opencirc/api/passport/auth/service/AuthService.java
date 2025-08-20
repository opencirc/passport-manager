package com.opencirc.api.passport.auth.service;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.LoginRequestDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import com.opencirc.api.passport.service.JwtService;
import com.opencirc.api.passport.service.PasswordService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {

    /**
     * Injecting UserRepository class.
     */
    private final UserRepository userRepository;

    /**
     * Injecting Properties class.
     */
    private final AppProperties properties;

    /**
     * Injecting PasswordService class.
     */
    private final PasswordService passwordService;

    /**
     * Injecting AuthenticationManager class.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Injecting JwtService class.
     */
    private final JwtService jwtService;

    /**
     * Injecting AuthUserDetailsService class.
     */
    private final AuthUserDetailsService authUserDetailsService;

    /**
     * Constructor.
     * @param userRepository
     * @param properties
     * @param passwordService
     * @param authenticationManager
     * @param jwtService
     * @param authUserDetailsService
     */
    public AuthService(UserRepository userRepository, AppProperties properties,
            PasswordService passwordService, AuthenticationManager authenticationManager,
            JwtService jwtService, AuthUserDetailsService authUserDetailsService) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.passwordService = passwordService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authUserDetailsService = authUserDetailsService;
    }
    /**
     * Register new user.
     *
     * @param email normalized email (trimmed, lowercased)
     * @param password raw password
     * @param firstName trimmed first name
     * @param lastName  trimmed last name
     * @param role desired role (defaults to USER if null)
     * @return the persisted User
     * @throws InvalidInputException if email or password are invalid
     * @throws AuthenticationException if email is already registered
     */
    @Transactional
    public User register(String email, String password, String firstName,
            String lastName, Role role) throws AuthenticationException {
        validateRegistrationFields(email, password);
        if (userRepository.existsByEmail(email)) {
            throw new AuthenticationException("A user by the provided email "
                    + "already exists: " + email);
        }

        User user = new User();

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role != null ? role : Role.USER);
        user.setActive(true);
        user.setPassword(passwordService.hashPassword(password));

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new AuthenticationException("A user by the provided email "
                    + "already exists: " + email);
        } catch (Exception e) {
            throw new AuthenticationException("Error during user registration.", e);
        }
    }
    /**
     * Validates registration fields (email and password).
     *
     * @param email     the user's email address
     * @param password  the user's password
     * @throws InvalidInputException if any validation rule fails
     */
    private void validateRegistrationFields(String email, String password) {
        EmailValidator emailValidator = EmailValidator.getInstance();
        if (!emailValidator.isValid(email)) {
            throw new InvalidInputException("Invalid email format.");
        }
        validatePassword(password);
    }
    /**
     * Checks whether the given password meets the complexity criteria. The password
     * must be at least 12 characters and contain at least three of the following
     * character types:Lower case, Upper case, Digits, Special characters
     *
     * @param password
     * @throws InvalidInputException if the password does not meet criteria
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 12) {
            throw new InvalidInputException("Password must be at least "
                    + "12 characters long.");
        }
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(
                c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        int classes = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0)
                + (hasSpecial ? 1 : 0);
        if (classes < 3) {
            throw new InvalidInputException(
                    "Weak password. Use at least 12 characters and mix of "
                            + "upper/lowercase, digits, or symbols.");
        }
    }

    /**
     * Login and verifies the user.
     *
     * @param loginRequest details with email, password
     * @param response
     * @return userDto the instance of UserDto
     */
    public UserDto login(LoginRequestDto loginRequest, HttpServletResponse response)
            throws AuthenticationException {

        final String email = normalizeEmail(loginRequest.getEmail());
        final String password = loginRequest.getPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException(
                    "Email or password must not be null or blank");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

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
     * Normalizes the given email by trimming whitespace and converting it to lowercase.
     * @param email
     * @return normalized email, or {@code null} if input is null
     */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
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
            String userId = jwtService.extractUserId(token);
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
        String userId = jwtService.extractUserId(refreshToken);
        User existingUser = userRepository.findById(UUID
                .fromString(userId)).orElseThrow(()
                -> new UsernameNotFoundException("User not found"));
        existingUser.setRefreshToken(null);
        userRepository.save(existingUser);

        // Removing the JWT cookies (access_token, refresh_token)
        jwtService.generateTokenCookie(response, "",
                AppConstants.COOKIE_ACCESS_TOKEN, 0);
        jwtService.generateTokenCookie(response, "",
                AppConstants.COOKIE_REFRESH_TOKEN, 0);
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
                if ("access_token".equals(cookie.getName())) {
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
