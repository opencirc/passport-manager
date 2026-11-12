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
import com.opencirc.api.passport.command.AdminCommand;
import com.opencirc.api.passport.dao.AdminRepository;
import com.opencirc.api.passport.model.Admin;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;

public class TestAdminCommand {

  public static AuthService registeringAuthService() {
    AuthService authService = mock(AuthService.class);
    when(authService.register(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(mock(Admin.class));
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
            .withStringInput(AdminCommand.PASSWORD_PROMPT_KEY)
            .name("Password")
            .maskCharacter('*')
            .and()
            .build()
            .run()
            .getContext()
            .<String>get(AdminCommand.PASSWORD_PROMPT_KEY))
        .thenReturn(promptedPassword);
    return componentFlowBuilder;
  }

  @Test
  public void registersWithPasswordOption() {
    AuthService authService = registeringAuthService();
    ComponentFlow.Builder componentFlowBuilder = mock(ComponentFlow.Builder.class);
    AdminCommand adminCommand =
        new AdminCommand(
            authService,
            mock(AdminRepository.class),
            componentFlowBuilder,
            terminalOfType("xterm"));

    String result =
        adminCommand.registerAdmin(
            "Smoke@Example.com", " Secret-Pass-123 ", "Smoke Test", "Boot Four");

    assertTrue(result.startsWith("Admin created with id: "), result);
    verify(authService)
        .register(
            eq("smoke@example.com"), eq("Secret-Pass-123"), eq("Smoke Test"), eq("Boot Four"));
    verify(componentFlowBuilder, never()).clone();
  }

  @Test
  public void promptsWithMaskWhenPasswordOptionIsOmitted() {
    AuthService authService = registeringAuthService();
    AdminCommand adminCommand =
        new AdminCommand(
            authService,
            mock(AdminRepository.class),
            promptAnswering("Prompted-Pass-123"),
            terminalOfType("xterm-256color"));

    String result =
        adminCommand.registerAdmin("smoke@example.com", null, "Smoke Test", "Boot Four");

    assertTrue(result.startsWith("Admin created with id: "), result);
    verify(authService)
        .register(
            eq("smoke@example.com"), eq("Prompted-Pass-123"), eq("Smoke Test"), eq("Boot Four"));
  }

  @Test
  public void keepsPromptedPasswordExactlyAsTyped() {
    AuthService authService = registeringAuthService();
    AdminCommand adminCommand =
        new AdminCommand(
            authService,
            mock(AdminRepository.class),
            promptAnswering(" Spaced-Pass-123 "),
            terminalOfType("xterm"));

    adminCommand.registerAdmin("smoke@example.com", null, "Smoke Test", "Boot Four");

    verify(authService)
        .register(
            eq("smoke@example.com"), eq(" Spaced-Pass-123 "), eq("Smoke Test"), eq("Boot Four"));
  }

  @Test
  public void refusesWithoutPasswordOnDumbTerminal() {
    AuthService authService = registeringAuthService();
    ComponentFlow.Builder componentFlowBuilder = promptAnswering("Should-Not-Be-Read");
    AdminCommand adminCommand =
        new AdminCommand(
            authService,
            mock(AdminRepository.class),
            componentFlowBuilder,
            terminalOfType(Terminal.TYPE_DUMB));

    String result = adminCommand.registerAdmin("smoke@example.com", "", "Smoke Test", "Boot Four");

    assertEquals(
        "No password given. Run from an interactive terminal to be prompted, or pass --password.",
        result);
    verify(authService, never()).register(any(), any(), any(), any());
    verify(componentFlowBuilder, never()).clone();
  }

  @Test
  public void refusesWhenPromptReturnsNothing() {
    AuthService authService = registeringAuthService();
    AdminCommand adminCommand =
        new AdminCommand(
            authService, mock(AdminRepository.class), promptAnswering(""), terminalOfType("xterm"));

    String result =
        adminCommand.registerAdmin("smoke@example.com", null, "Smoke Test", "Boot Four");

    assertTrue(result.startsWith("No password given."), result);
    verify(authService, never()).register(any(), any(), any(), any());
  }
}
