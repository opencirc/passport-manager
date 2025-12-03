package com.opencirc.api.passport.command;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import com.opencirc.api.passport.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(group = "Register User Commands")
@Slf4j
public class RegisterUserCommand {

  /** Injecting AuthService. */
  private final AuthService authService;

  /** Constructor. */
  public RegisterUserCommand(AuthService authService) {
    this.authService = authService;
  }

  /** Shell command to register a new user. */
  @Command(description = "Register a user.")
  public String register(
      @Option(longNames = "email", required = true) String email,
      @Option(longNames = "password", required = true) String password,
      @Option(longNames = "firstName", required = true) String firstName,
      @Option(longNames = "lastName", required = true) String lastName,
      @Option(longNames = "role", defaultValue = "user") String role) {

    Role parsedRole;
    try {
      parsedRole = Role.fromValue(role.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return "Invalid role. Valid roles are 'user' or 'admin'.";
    }

    try {
      User user =
          authService.register(
              StringUtil.normalizeEmail(email),
              password.trim(),
              firstName.trim(),
              lastName.trim(),
              parsedRole);
      return String.format("User created with id: %s", user.getId());
    } catch (AuthenticationException authenticationException) {
      throw authenticationException;
    } catch (Exception e) {
      throw e;
    }
  }
}
