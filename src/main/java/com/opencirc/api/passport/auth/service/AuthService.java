package com.opencirc.api.passport.auth.service;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.AdminRepository;
import com.opencirc.api.passport.dto.AdminDto;
import com.opencirc.api.passport.dto.LoginRequestDto;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.Admin;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.JwtService;
import com.opencirc.api.passport.service.PasswordService;
import com.opencirc.api.passport.util.StringUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

/** Service class for administrator authentication. */
@Service
public class AuthService {

  private final AdminRepository adminRepository;

  private final AppProperties properties;

  private final PasswordService passwordService;

  private final AuthenticationManager authenticationManager;

  private final JwtService jwtService;

  private final AdminDetailsService adminDetailsService;

  private final ApiKeyService apiKeyService;

  /** Constructor. */
  public AuthService(
      AdminRepository adminRepository,
      AppProperties properties,
      PasswordService passwordService,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      AdminDetailsService adminDetailsService,
      ApiKeyService apiKeyService) {
    this.adminRepository = adminRepository;
    this.properties = properties;
    this.passwordService = passwordService;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.adminDetailsService = adminDetailsService;
    this.apiKeyService = apiKeyService;
  }

  /** Registers a new administrator. */
  @Transactional
  public Admin register(String email, String password, String firstName, String lastName)
      throws AuthenticationException {
    validateRegistrationFields(email, password);
    if (adminRepository.existsByEmail(email)) {
      throw new AuthenticationException(
          "An admin by the provided email " + "already exists: " + email);
    }

    Admin admin = new Admin();

    admin.setEmail(email);
    admin.setFirstName(firstName);
    admin.setLastName(lastName);
    admin.setActive(true);
    admin.setPassword(passwordService.hashPassword(password));

    try {
      return adminRepository.save(admin);
    } catch (DataIntegrityViolationException e) {
      throw new AuthenticationException(
          "An admin by the provided email " + "already exists: " + email);
    } catch (Exception e) {
      throw new AuthenticationException("Error during admin registration.", e);
    }
  }

  /** Validates registration fields (email and password). */
  private void validateRegistrationFields(String email, String password) {
    EmailValidator emailValidator = EmailValidator.getInstance();
    if (!emailValidator.isValid(email)) {
      throw new InvalidInputException("Invalid email format.");
    }
    validatePassword(password);
  }

  /**
   * Checks whether the given password meets the complexity criteria. The password must be at least
   * 12 characters and contain at least three of the following character types:Lower case, Upper
   * case, Digits, Special characters
   */
  private void validatePassword(String password) {
    if (password == null || password.length() < 12) {
      throw new InvalidInputException("Password must be at least " + "12 characters long.");
    }
    boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
    boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    boolean hasSpecial =
        password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    int classes =
        (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
    if (classes < 3) {
      throw new InvalidInputException(
          "Weak password. Use at least 12 characters and mix of "
              + "upper/lowercase, digits, or symbols.");
    }
  }

  /** Verifies the administrator and logs them in. */
  public AdminDto login(LoginRequestDto loginRequest, HttpServletResponse response) {

    final String email = StringUtil.normalizeEmail(loginRequest.getEmail());

    final String password = loginRequest.getPassword();
    if (password == null || password.isBlank()) {
      throw new InvalidInputException("Password cannot be null or blank");
    }

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(email, password));

      if (!authentication.isAuthenticated()) {
        throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
      }

      AdminPrincipal adminPrincipal = (AdminPrincipal) authentication.getPrincipal();
      String adminId = adminPrincipal.getAdminId();

      String accessToken = jwtService.generateToken(adminId, properties.getAccessTokenExpiryTime());
      String refreshToken =
          jwtService.generateToken(adminId, properties.getRefreshTokenExpiryTime());

      adminRepository.updateRefreshTokenById(adminId, refreshToken);

      jwtService.generateTokenCookie(
          response,
          accessToken,
          AppConstants.COOKIE_ACCESS_TOKEN,
          properties.getAccessTokenExpiryTime());
      jwtService.generateTokenCookie(
          response,
          refreshToken,
          AppConstants.COOKIE_REFRESH_TOKEN,
          properties.getRefreshTokenExpiryTime());

      return AdminDto.from(adminPrincipal);

    } catch (BadCredentialsException ex) {
      throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
    }
  }

  /** Refreshes the expired token. */
  public String refreshToken(String refreshToken, HttpServletResponse response)
      throws AuthenticationException {
    try {
      String newAccessToken = jwtService.generateAccessTokenUsingRefreshToken(refreshToken);
      jwtService.generateTokenCookie(
          response,
          newAccessToken,
          AppConstants.COOKIE_ACCESS_TOKEN,
          properties.getAccessTokenExpiryTime());
      return newAccessToken;
    } catch (Exception e) {
      throw new AuthenticationException("Error refreshing token: " + e.getMessage());
    }
  }

  /** Validates the token. */
  public boolean validateToken(String token) {
    try {
      String adminId = jwtService.extractUserId(token);
      UserDetails userDetails = adminDetailsService.loadUserById(adminId);

      return jwtService.validateToken(token, userDetails);
    } catch (Exception e) {
      throw new AuthenticationException("Error validating token: " + e.getMessage());
    }
  }

  /** Logs the administrator out. */
  public void logout(String refreshToken, HttpServletResponse response) {
    SecurityContextHolder.clearContext();
    String adminId = jwtService.extractUserId(refreshToken);
    Admin existingAdmin =
        adminRepository
            .findById(adminId)
            .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
    existingAdmin.setRefreshToken(null);
    adminRepository.save(existingAdmin);

    // Removing the JWT cookies (access_token, refresh_token)
    jwtService.generateTokenCookie(response, "", AppConstants.COOKIE_ACCESS_TOKEN, 0);
    jwtService.generateTokenCookie(response, "", AppConstants.COOKIE_REFRESH_TOKEN, 0);
  }

  /** Gets the details of the currently logged in administrator, or null for machine requests. */
  public AdminDto getCurrentAdmin(HttpServletRequest request) {

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
    Admin admin = null;

    if (token != null && validateToken(token)) {
      String adminId = jwtService.extractUserId(token);
      admin = adminRepository.findById(adminId).orElse(null);
    }

    return admin != null ? AdminDto.from(admin) : null;
  }

  /** Whether the request carries a valid machine token. */
  public boolean validateApiKey(HttpServletRequest request) {
    String token = ApiKeyService.resolveToken(request);
    if (token == null) {
      return false;
    }
    try {
      apiKeyService.authenticate(token);
      return true;
    } catch (AuthenticationException e) {
      return false;
    }
  }
}
