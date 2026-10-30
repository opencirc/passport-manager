package com.opencirc.api.passport.command.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.command.UserCommand;
import com.opencirc.api.passport.dao.UserRepository;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;

public class TestUserCommand {

  public static AuthService registeringAuthService() {
    AuthService authService = mock(AuthService.class);
    when(authService.register(anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(mock(User.class));
    return authService;
  }

  public static Terminal terminalOfType(String terminalType) {
    Terminal terminal = mock(Terminal.class);
    when(terminal.getType()).thenReturn(terminalType);
    return terminal;
  }

  /** A component flow builder whose masked string input answers with the given password. */
  public static ComponentFlow.Builder promptAnswering(String promptedPassword) {
    ComponentFlow.Builder componentFlowBuilder =
        mock(ComponentFlow.Builder.class, RETURNS_DEEP_STUBS);
    // Deep stubs leave Object methods such as clone() unstubbed, so answer it explicitly.
    when(componentFlowBuilder.clone()).thenReturn(componentFlowBuilder);
    when(componentFlowBuilder
            .withStringInput(UserCommand.PASSWORD_PROMPT_KEY)
            .name("Password")
            .maskCharacter('*')
            .and()
            .build()
            .run()
            .getContext()
            .<String>get(UserCommand.PASSWORD_PROMPT_KEY))
        .thenReturn(promptedPassword);
    return componentFlowBuilder;
  }

  @Test
  public void registersWithPasswordOption() {
    AuthService authService = registeringAuthService();
    ComponentFlow.Builder componentFlowBuilder = mock(ComponentFlow.Builder.class);
    UserCommand userCommand =
        new UserCommand(
            authService, mock(UserRepository.class), componentFlowBuilder, terminalOfType("xterm"));

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
    verify(componentFlowBuilder, never()).clone();
  }

  @Test
  public void promptsWithMaskWhenPasswordOptionIsOmitted() {
    AuthService authService = registeringAuthService();
    UserCommand userCommand =
        new UserCommand(
            authService,
            mock(UserRepository.class),
            promptAnswering("Prompted-Pass-123"),
            terminalOfType("xterm-256color"));

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
  public void keepsPromptedPasswordExactlyAsTyped() {
    AuthService authService = registeringAuthService();
    UserCommand userCommand =
        new UserCommand(
            authService,
            mock(UserRepository.class),
            promptAnswering(" Spaced-Pass-123 "),
            terminalOfType("xterm"));

    userCommand.registerUser("smoke@example.com", null, "Smoke Test", "Boot Four", "user");

    verify(authService)
        .register(
            eq("smoke@example.com"),
            eq(" Spaced-Pass-123 "),
            eq("Smoke Test"),
            eq("Boot Four"),
            eq(Role.USER));
  }

  @Test
  public void refusesWithoutPasswordOnDumbTerminal() {
    AuthService authService = registeringAuthService();
    ComponentFlow.Builder componentFlowBuilder = promptAnswering("Should-Not-Be-Read");
    UserCommand userCommand =
        new UserCommand(
            authService,
            mock(UserRepository.class),
            componentFlowBuilder,
            terminalOfType(Terminal.TYPE_DUMB));

    String result =
        userCommand.registerUser("smoke@example.com", "", "Smoke Test", "Boot Four", "user");

    assertEquals(
        "No password given. Run from an interactive terminal to be prompted, or pass --password.",
        result);
    verify(authService, never()).register(any(), any(), any(), any(), any());
    verify(componentFlowBuilder, never()).clone();
  }

  @Test
  public void refusesWhenPromptReturnsNothing() {
    AuthService authService = registeringAuthService();
    UserCommand userCommand =
        new UserCommand(
            authService, mock(UserRepository.class), promptAnswering(""), terminalOfType("xterm"));

    String result =
        userCommand.registerUser("smoke@example.com", null, "Smoke Test", "Boot Four", "user");

    assertTrue(result.startsWith("No password given."), result);
    verify(authService, never()).register(any(), any(), any(), any(), any());
  }

  @Test
  public void rejectsUnknownRoleBeforePrompting() {
    AuthService authService = registeringAuthService();
    ComponentFlow.Builder componentFlowBuilder = promptAnswering("Prompted-Pass-123");
    UserCommand userCommand =
        new UserCommand(
            authService, mock(UserRepository.class), componentFlowBuilder, terminalOfType("xterm"));

    String result =
        userCommand.registerUser("smoke@example.com", null, "Smoke Test", "Boot Four", "root");

    assertEquals("Invalid role. Valid roles are 'user' or 'admin'.", result);
    verify(authService, never()).register(any(), any(), any(), any(), any());
    verify(componentFlowBuilder, never()).clone();
  }
}
