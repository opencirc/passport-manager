package com.opencirc.api.passport.config;

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
   * so the shell only runs when interactive mode is on or a command was passed.
   */
  @Bean(name = "springShellApplicationRunner")
  public ApplicationRunner springShellApplicationRunner(
      ShellRunner shellRunner,
      @Value("${spring.shell.interactive.enabled:true}") boolean interactiveShellEnabled) {
    return applicationArguments -> {
      String[] sourceArguments = applicationArguments.getSourceArgs();
      if (interactiveShellEnabled || sourceArguments.length > 0) {
        shellRunner.run(sourceArguments);
      }
    };
  }
}
