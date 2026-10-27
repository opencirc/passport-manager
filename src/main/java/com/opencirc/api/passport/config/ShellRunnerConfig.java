package com.opencirc.api.passport.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.ShellRunner;

/** Spring Shell runner wiring. */
@Configuration
public class ShellRunnerConfig {

  /**
   * Replaces Spring Shell's default application runner. Spring Shell 4 logs an error when the
   * non-interactive runner starts without a command, which is the normal case for the web server,
   * so the shell only runs when interactive mode is on or a command was passed. Non-interactive
   * arguments are re-quoted first because Spring Shell 4 joins them with spaces before parsing.
   */
  @Bean(name = "springShellApplicationRunner")
  public ApplicationRunner springShellApplicationRunner(
      ShellRunner shellRunner,
      @Value("${spring.shell.interactive.enabled:true}") boolean interactiveShellEnabled) {
    return applicationArguments -> {
      String[] sourceArguments = applicationArguments.getSourceArgs();
      if (interactiveShellEnabled) {
        shellRunner.run(sourceArguments);
      } else if (sourceArguments.length > 0) {
        shellRunner.run(quoteNonInteractiveArguments(sourceArguments));
      }
    };
  }

  /**
   * Restores the argument boundaries the operating system shell already established. Spring Shell
   * 4's non-interactive runner joins the arguments with spaces and re-splits on whitespace outside
   * double quotes, so a value containing whitespace must be wrapped in double quotes again, as
   * Spring Shell 3 did. A leading script reference ({@code @file}) is passed through unchanged.
   */
  public static String[] quoteNonInteractiveArguments(String[] sourceArguments) {
    String[] quotedArguments = Arrays.copyOf(sourceArguments, sourceArguments.length);
    for (int argumentIndex = 0; argumentIndex < sourceArguments.length; argumentIndex++) {
      String argument = sourceArguments[argumentIndex];
      if (argumentIndex == 0 && argument.startsWith("@")) {
        continue;
      }
      quotedArguments[argumentIndex] = quoteArgument(argument);
    }
    return quotedArguments;
  }

  /** Quotes the whole argument, or only the value of an {@code --option=value} argument. */
  public static String quoteArgument(String argument) {
    if (argument.startsWith("--") && argument.contains("=")) {
      int separatorIndex = argument.indexOf('=');
      String optionName = argument.substring(0, separatorIndex + 1);
      String optionValue = argument.substring(separatorIndex + 1);
      return optionName + quoteValue(optionValue);
    }
    return quoteValue(argument);
  }

  /** Wraps a value in double quotes when it needs them and is not already quoted. */
  public static String quoteValue(String value) {
    boolean alreadyQuoted = value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"");
    boolean needsQuotes = value.chars().anyMatch(Character::isWhitespace) || value.contains("\"");
    if (alreadyQuoted || !needsQuotes) {
      return value;
    }
    String escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"");
    return "\"" + escapedValue + "\"";
  }
}
