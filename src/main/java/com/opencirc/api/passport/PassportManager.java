package com.opencirc.api.passport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.opencirc.api.passport.*")
public class PassportManager {

  /** Main class. */
  public static void main(String[] args) {

    SpringApplication.run(PassportManager.class, args);
  }
}
