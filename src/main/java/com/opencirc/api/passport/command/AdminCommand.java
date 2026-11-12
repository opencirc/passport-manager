package com.opencirc.api.passport.command;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.dao.AdminRepository;
import com.opencirc.api.passport.model.Admin;
import com.opencirc.api.passport.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.Terminal;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;

/** Commands to manage administrator accounts. */
@CommandGroup(name = "Admin commands")
@Slf4j
public class AdminCommand {

  private static final String ADMIN_LIST_ROW_FORMAT = "%-40s %-30s %-30s%n";

  public static final String PASSWORD_PROMPT_KEY = "password";

  private final AuthService authService;

  private final AdminRepository adminRepository;

  private final ComponentFlow.Builder componentFlowBuilder;

  private final Terminal terminal;

  /** Constructor. */
  public AdminCommand(
      AuthService authService,
      AdminRepository adminRepository,
      ComponentFlow.Builder componentFlowBuilder,
      Terminal terminal) {
    this.authService = authService;
    this.adminRepository = adminRepository;
    this.componentFlowBuilder = componentFlowBuilder;
    this.terminal = terminal;
  }

  /**
   * Shell command to register a new administrator. The password is prompted for when the option is
   * omitted, so it does not end up in the process list or the shell history.
   */
  @Command(description = "Register an administrator.")
  public String registerAdmin(
      @Option(longName = "email", required = true) String email,
      @Option(
              longName = "password",
              description = "Omit to be prompted without echo (recommended).")
          String password,
      @Option(longName = "firstName", required = true) String firstName,
      @Option(longName = "lastName", required = true) String lastName) {

    // Only an explicit option is trimmed; a prompted password is kept exactly as typed.
    String resolvedPassword;
    if (password == null || password.isBlank()) {
      resolvedPassword = promptForPassword();
    } else {
      resolvedPassword = password.trim();
    }
    if (resolvedPassword == null || resolvedPassword.isEmpty()) {
      return "No password given. Run from an interactive terminal to be prompted, or pass"
          + " --password.";
    }

    Admin admin =
        authService.register(
            StringUtil.normalizeEmail(email), resolvedPassword, firstName.trim(), lastName.trim());
    return String.format("Admin created with id: %s", admin.getId());
  }

  /**
   * Prompts for the password on the shell's terminal with the input masked. Returns null when the
   * terminal is not interactive (JLine falls back to a dumb terminal when input or output is
   * piped), so the caller refuses instead of reading a password from a pipe or echoing it.
   */
  public String promptForPassword() {
    String terminalType = terminal.getType();
    if (Terminal.TYPE_DUMB.equals(terminalType) || Terminal.TYPE_DUMB_COLOR.equals(terminalType)) {
      return null;
    }
    ComponentFlow passwordFlow =
        componentFlowBuilder
            .clone()
            .withStringInput(PASSWORD_PROMPT_KEY)
            .name("Password")
            .maskCharacter('*')
            .and()
            .build();
    return passwordFlow.run().getContext().get(PASSWORD_PROMPT_KEY);
  }

  /** Shell command to list existing administrators. */
  @Command(description = "List administrators.")
  public String listAdmins() {
    StringBuilder sb = new StringBuilder();
    for (var admin : adminRepository.findAll()) {
      sb.append(
          String.format(
              ADMIN_LIST_ROW_FORMAT, admin.getId(), admin.getFullName(), admin.getEmail()));
    }

    return sb.toString();
  }
}
