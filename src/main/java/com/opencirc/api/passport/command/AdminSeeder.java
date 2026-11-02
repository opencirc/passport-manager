package com.opencirc.api.passport.command;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Seeder for creating the initial test administrator. */
@Component
@Slf4j
public class AdminSeeder {

  /** Service for handling authentication and registration. */
  private final AuthService authService;

  /** Injecting Properties class. */
  private final AppProperties appProperties;

  /** Constructor-based dependency injection for AuthService. */
  public AdminSeeder(AuthService authService, AppProperties appProperties) {
    this.authService = authService;
    this.appProperties = appProperties;
  }

  /** Seeds the predefined test administrator. */
  public void seed() {
    registerSafely("admin@test.com", "test", "admin");
    log.info("Admin seeding completed.");
  }

  /** Registers a new administrator. Skips if already present. */
  private void registerSafely(String email, String firstName, String lastName) {
    try {
      authService.register(email, appProperties.getDefaultSeedPassword(), firstName, lastName);
    } catch (Exception e) {
      if (e.getMessage() != null && e.getMessage().toLowerCase().contains("exists")) {
        log.info("Admin '{}' already exists. Skipping.", email);
        return;
      }
      throw e;
    }
  }
}
