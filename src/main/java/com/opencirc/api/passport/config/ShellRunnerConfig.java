package com.opencirc.api.passport.config;

import java.util.Arrays;
import java.util.regex.Pattern;
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

  /** The word splitting Spring Shell 4.0.3's DefaultCommandParser applies to the joined input. */
  public static final Pattern SPRING_SHELL_WORD_SEPARATOR =
      Pattern.compile("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

  /**
   * Restores the argument boundaries the operating system shell already established. Spring Shell
   * 4's non-interactive runner joins the arguments with spaces and re-splits on whitespace outside
   * double quotes, so a value containing whitespace must be wrapped in double quotes again, as
   * Spring Shell 3 did. A leading script reference ({@code @file}) is used as a file path upstream
   * and is passed through unchanged. Arguments that Spring Shell would still split after quoting
   * are rejected, so a command never runs with a silently altered value such as a password.
   */
  public static String[] quoteNonInteractiveArguments(String[] sourceArguments) {
    if (sourceArguments.length > 0 && sourceArguments[0].startsWith("@")) {
      return Arrays.copyOf(sourceArguments, sourceArguments.length);
    }
    String[] quotedArguments = new String[sourceArguments.length];
    for (int argumentIndex = 0; argumentIndex < sourceArguments.length; argumentIndex++) {
      quotedArguments[argumentIndex] = quoteArgument(sourceArguments[argumentIndex]);
    }
    rejectArgumentsSpringShellWouldSplit(quotedArguments);
    return quotedArguments;
  }

  /**
   * Fails when re-splitting the joined arguments does not give the arguments back. Spring Shell
   * 4.0.3 counts raw quote characters when splitting and ignores escapes, so a value containing
   * both whitespace and an even number of literal quotes after one of its spaces would be split
   * into several words. The values are deliberately not included in the message.
   */
  public static void rejectArgumentsSpringShellWouldSplit(String[] quotedArguments) {
    String[] resplitArguments =
        SPRING_SHELL_WORD_SEPARATOR.split(String.join(" ", quotedArguments));
    if (Arrays.equals(resplitArguments, quotedArguments)) {
      return;
    }
    for (int argumentIndex = 0; argumentIndex < quotedArguments.length; argumentIndex++) {
      String[] resplitArgument = SPRING_SHELL_WORD_SEPARATOR.split(quotedArguments[argumentIndex]);
      if (resplitArgument.length != 1) {
        throw new IllegalArgumentException(
            "Argument "
                + (argumentIndex + 1)
                + " combines whitespace and double quotes in a way Spring Shell's non-interactive"
                + " parser cannot pass on intact; use the interactive shell for this value.");
      }
    }
    throw new IllegalArgumentException(
        "The command line cannot be passed on intact by Spring Shell's non-interactive parser;"
            + " use the interactive shell for these values.");
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

  /**
   * Wraps a value in double quotes when it contains whitespace or quotes. Literal quotes, including
   * a pair at both ends, are part of the value the operating system shell already resolved, so they
   * are escaped rather than treated as Spring Shell quoting. Known limitation: Spring Shell 4.0.3
   * splits words by counting raw quote characters, ignoring escapes, so a value that contains both
   * whitespace and an even number of literal quotes after one of its spaces is still split
   * upstream.
   */
  public static String quoteValue(String value) {
    boolean needsQuotes = value.chars().anyMatch(Character::isWhitespace) || value.contains("\"");
    if (!needsQuotes) {
      return value;
    }
    String escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"");
    return "\"" + escapedValue + "\"";
  }
}
