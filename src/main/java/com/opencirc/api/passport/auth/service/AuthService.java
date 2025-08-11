package com.opencirc.api.passport.auth.service;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.dto.LoginRequestDto;
import com.opencirc.api.passport.dto.RegisterUserDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import com.opencirc.api.passport.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder(12);

  @Autowired private UserRepository userRepository;

  @Autowired private AppProperties properties;

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired private JwtService jwtService;

  @Autowired private AuthUserDetailsService authUserDetailsService;

  /** Create a new user with the provided information. */
  public User register(RegisterUserDto registerUser) throws AuthenticationException {

    User user = new User();

    user.setUsername(registerUser.getUsername());
    user.setPassword(registerUser.getPassword());
    user.setEmail(registerUser.getEmail());
    user.setRole(Role.USER);
    user.setActive(true);
    user.setCreatedTime(registerUser.getCreatedTime());

    if (userRepository.existsByUsername(user.getUsername())) {
      throw new AuthenticationException(AppConstants.ERR_USERNAME_EXISTS);
    }

    String encodedPassword = bcryptPasswordEncoder.encode(user.getPassword());
    user.setPassword(encodedPassword);

    return userRepository.save(user);
  }

  /** Uses the login information in the login request to authenticate the user. */
  public UserDto login(LoginRequestDto loginRequest, HttpServletResponse response)
      throws AuthenticationException {
    if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
      throw new AuthenticationException("Username or password must not be null");
    }

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getUsername(), loginRequest.getPassword()));

      if (!authentication.isAuthenticated()) {
        throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
      }

      UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
      String userId = userPrincipal.getUserId();

      String accessToken = jwtService.generateToken(userId, properties.getAccessTokenExpiryTime());
      String refreshToken =
          jwtService.generateToken(userId, properties.getRefreshTokenExpiryTime());

      userRepository.updateRefreshTokenById(UUID.fromString(userId), refreshToken);

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

      return UserDto.from(userPrincipal);

    } catch (BadCredentialsException ex) {
      throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
    }
  }

  /** Refreshes the expired token. */
  public void refreshToken(String refreshToken, HttpServletResponse response)
      throws AuthenticationException {
    String newAccessToken = jwtService.generateAccessTokenUsingRefreshToken(refreshToken);
    jwtService.generateTokenCookie(
        response,
        newAccessToken,
        AppConstants.COOKIE_ACCESS_TOKEN,
        properties.getAccessTokenExpiryTime());
  }

  /**
   * Validates the provided JWT token.
   */
  public boolean validateToken(String token) {
    String userId = jwtService.extractUserId(token);
    UserDetails userDetails = authUserDetailsService.loadUserById(userId);

    return jwtService.validateToken(token, userDetails);
  }

  /**
   * Logs the user out by clearing the security context and removing the JWT cookies.
   */
  public void logout(String refreshToken, HttpServletResponse response) {
    SecurityContextHolder.clearContext();
    String userId = jwtService.extractUserId(refreshToken);
    User existingUser =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    existingUser.setRefreshToken(null);
    userRepository.save(existingUser);

    // Removing the JWT cookies (access_token, refresh_token)
    jwtService.generateTokenCookie(response, "", AppConstants.COOKIE_ACCESS_TOKEN, 0);
    jwtService.generateTokenCookie(response, "", AppConstants.COOKIE_REFRESH_TOKEN, 0);
  }

  /**
   * Gets the details of the currently logged-in user, identified by the ID in the JWT token.
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
