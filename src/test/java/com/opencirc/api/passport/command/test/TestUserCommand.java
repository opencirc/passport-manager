package com.opencirc.api.passport.command.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.command.UserCommand;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import org.junit.jupiter.api.Test;

public class TestUserCommand {

  /** Command whose console prompt is replaced by a fixed answer. */
  public static class PromptingUserCommand extends UserCommand {
    public final String promptedPassword;

    public PromptingUserCommand(AuthService authService, String promptedPassword) {
      super(authService, mock(UserRepository.class));
      this.promptedPassword = promptedPassword;
    }

    @Override
    public String readPasswordFromConsole() {
      return promptedPassword;
    }
  }

  @Test
  public void registersWithPasswordOption() {
    AuthService authService = mock(AuthService.class);
    when(authService.register(anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(mock(User.class));
    UserCommand userCommand = new PromptingUserCommand(authService, "should not be used");

    String result =
        userCommand.registerUser(
            "Smoke@Example.com", " Secret-Pass-123 ", "Smoke Test", "Boot Four", "admin");

    assertTrue(result.startsWith("User created with id: "), result);
    verify(authService)
        .register(
            eq("smoke@example.com"),
            eq("Secret-Pass-123"),
            eq("Smoke Test"),
            eq("Boot Four"),
            eq(Role.ADMIN));
  }

  @Test
  public void promptsWhenPasswordOptionIsOmitted() {
    AuthService authService = mock(AuthService.class);
    when(authService.register(anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(mock(User.class));
    UserCommand userCommand = new PromptingUserCommand(authService, "Prompted-Pass-123");

    String result =
        userCommand.registerUser("smoke@example.com", null, "Smoke Test", "Boot Four", "user");

    assertTrue(result.startsWith("User created with id: "), result);
    verify(authService)
        .register(
            eq("smoke@example.com"),
            eq("Prompted-Pass-123"),
            eq("Smoke Test"),
            eq("Boot Four"),
            eq(Role.USER));
  }

  @Test
  public void refusesWithoutPasswordAndWithoutTerminal() {
    AuthService authService = mock(AuthService.class);
    UserCommand userCommand = new PromptingUserCommand(authService, null);

    String result =
        userCommand.registerUser("smoke@example.com", "", "Smoke Test", "Boot Four", "user");

    assertEquals(
        "No password given. Run from a terminal to be prompted, or pass --password.", result);
    verify(authService, never()).register(any(), any(), any(), any(), any());
  }

  @Test
  public void rejectsUnknownRoleBeforeAskingForPassword() {
    AuthService authService = mock(AuthService.class);
    UserCommand userCommand = new PromptingUserCommand(authService, "Prompted-Pass-123");

    String result =
        userCommand.registerUser("smoke@example.com", null, "Smoke Test", "Boot Four", "root");

    assertEquals("Invalid role. Valid roles are 'user' or 'admin'.", result);
    verify(authService, never()).register(any(), any(), any(), any(), any());
  }
}
