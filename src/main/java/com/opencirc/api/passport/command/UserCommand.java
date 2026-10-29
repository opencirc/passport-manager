package com.opencirc.api.passport.command;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import com.opencirc.api.passport.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;

/** Commands to interact with users. */
@CommandGroup(name = "User commands")
@Slf4j
public class UserCommand {

  private static final String USER_LIST_ROW_FORMAT = "%-40s %-20s %-25s%n";

  private final AuthService authService;

  private final UserRepository userRepository;

  /** Constructor. */
  public UserCommand(AuthService authService, UserRepository userRepository) {
    this.authService = authService;
    this.userRepository = userRepository;
  }

  /**
   * Shell command to register a new user. The password is prompted for when the option is omitted,
   * so it does not end up in the process list or the shell history.
   */
  @Command(description = "Register a user.")
  public String registerUser(
      @Option(longName = "email", required = true) String email,
      @Option(
              longName = "password",
              description = "Omit to be prompted without echo (recommended).")
          String password,
      @Option(longName = "firstName", required = true) String firstName,
      @Option(longName = "lastName", required = true) String lastName,
      @Option(longName = "role", defaultValue = "user") String role) {

    Role parsedRole;
    try {
      parsedRole = Role.fromValue(role.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return "Invalid role. Valid roles are 'user' or 'admin'.";
    }

    String resolvedPassword = password;
    if (resolvedPassword == null || resolvedPassword.isBlank()) {
      resolvedPassword = readPasswordFromConsole();
    }
    if (resolvedPassword == null || resolvedPassword.isBlank()) {
      return "No password given. Run from a terminal to be prompted, or pass --password.";
    }

    User user =
        authService.register(
            StringUtil.normalizeEmail(email),
            resolvedPassword.trim(),
            firstName.trim(),
            lastName.trim(),
            parsedRole);
    return String.format("User created with id: %s", user.getId());
  }

  /**
   * Prompts for the password on the attached terminal without echoing it. Returns null when no
   * terminal is attached, for example when the output is piped.
   */
  public String readPasswordFromConsole() {
    java.io.Console console = System.console();
    if (console == null) {
      return null;
    }
    char[] passwordCharacters = console.readPassword("Password: ");
    if (passwordCharacters == null) {
      return null;
    }
    String enteredPassword = new String(passwordCharacters);
    java.util.Arrays.fill(passwordCharacters, '\0');
    return enteredPassword;
  }

  /** Shell command to list existing users. */
  @Command(description = "List users.")
  public String listUsers() {
    StringBuilder sb = new StringBuilder();
    for (var user : userRepository.findAll()) {
      sb.append(
          String.format(USER_LIST_ROW_FORMAT, user.getId(), user.getFullName(), user.getRole()));
    }

    return sb.toString();
  }
}
