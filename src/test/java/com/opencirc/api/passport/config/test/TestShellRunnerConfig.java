package com.opencirc.api.passport.config.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.opencirc.api.passport.config.ShellRunnerConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.ParsedInput;

/** Tests the argument handling of the non-interactive shell runner. */
public class TestShellRunnerConfig {

  public final ShellRunnerConfig shellRunnerConfig = new ShellRunnerConfig();

  @Test
  public void shouldQuoteMultiwordValuesInNonInteractiveMode() throws Exception {
    ShellRunner shellRunner = mock(ShellRunner.class);
    ArgumentCaptor<String[]> forwardedArguments = ArgumentCaptor.forClass(String[].class);

    shellRunnerConfig
        .springShellApplicationRunner(shellRunner, false)
        .run(
            new DefaultApplicationArguments(
                "create-api-key", "--userId", "42", "--name", "Staging integration"));

    verify(shellRunner).run(forwardedArguments.capture());
    assertArrayEquals(
        new String[] {"create-api-key", "--userId", "42", "--name", "\"Staging integration\""},
        forwardedArguments.getValue());
  }

  @Test
  public void shouldParseQuotedMultiwordValueAsSingleOptionWithSpringShellParser() {
    String[] quotedArguments =
        ShellRunnerConfig.quoteNonInteractiveArguments(
            new String[] {"create-api-key", "--userId", "42", "--name", "Staging integration"});
    DefaultCommandParser commandParser = new DefaultCommandParser(mock(CommandRegistry.class));

    ParsedInput parsedInput = commandParser.parse(String.join(" ", quotedArguments));

    assertEquals("create-api-key", parsedInput.commandName());
    assertTrue(parsedInput.arguments().isEmpty(), "no extra positional argument expected");
    assertEquals(2, parsedInput.options().size());
    assertEquals("Staging integration", parsedInput.options().get(1).value());
  }

  @Test
  public void shouldQuoteOnlyTheValueOfAnEqualsOption() {
    assertEquals(
        "--name=\"Staging integration\"",
        ShellRunnerConfig.quoteArgument("--name=Staging integration"));
  }

  @Test
  public void shouldEscapeEmbeddedQuotesAndBackslashes() {
    assertEquals("\"say \\\"hi\\\" \\\\ bye\"", ShellRunnerConfig.quoteValue("say \"hi\" \\ bye"));
  }

  @Test
  public void shouldLeavePlainQuotedAndScriptArgumentsUnchanged() {
    assertArrayEquals(
        new String[] {"@my script.txt", "plain", "\"already quoted\""},
        ShellRunnerConfig.quoteNonInteractiveArguments(
            new String[] {"@my script.txt", "plain", "\"already quoted\""}));
  }

  @Test
  public void shouldForwardRawArgumentsInInteractiveMode() throws Exception {
    ShellRunner shellRunner = mock(ShellRunner.class);
    String[] sourceArguments = {"--name", "Staging integration"};

    shellRunnerConfig
        .springShellApplicationRunner(shellRunner, true)
        .run(new DefaultApplicationArguments(sourceArguments));

    verify(shellRunner).run(sourceArguments);
  }

  @Test
  public void shouldNotRunTheShellWithoutArgumentsInNonInteractiveMode() throws Exception {
    ShellRunner shellRunner = mock(ShellRunner.class);

    shellRunnerConfig
        .springShellApplicationRunner(shellRunner, false)
        .run(new DefaultApplicationArguments());

    verify(shellRunner, never()).run(any());
  }
}
