package com.opencirc.api.passport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.opencirc.api.passport.*")
@CommandScan
public class PassportManager {

  /**
   * Main class.
   *
   * @param args
   */
  public static void main(String[] args) {

    SpringApplication.run(PassportManager.class, args);
  }
}
