package com.opencirc.api.passport.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * Seeder command entry point for running seed operations. Supports seeding of users, passports, or
 * both.
 */
@Command(group = "Seeder Commands")
@Slf4j
@Component
public class Seeder {

  /** UserSeeder bean. */
  private final UserSeeder userSeeder;

  /** Passport seeder via data dictionary API. */
  private final PassportFromApiSeeder passportFromApiSeeder;

  /** Passport seeder from stored JSON templates. */
  private final PassportFromJsonSeeder passportFromJsonSeeder;

  /**
   * Constructor-based dependency injection for seeder components.
   */
  public Seeder(
      UserSeeder userSeeder,
      PassportFromApiSeeder passportFromApiSeeder,
      PassportFromJsonSeeder passportFromJsonSeeder) {
    this.userSeeder = userSeeder;
    this.passportFromApiSeeder = passportFromApiSeeder;
    this.passportFromJsonSeeder = passportFromJsonSeeder;
  }

  /** Enum representing supported seed types. */
  private enum SeedType {

    /** Seed only user-related data. */
    USER,

    /** Seed passport-related data from API. */
    PASSPORT_FROM_API,

    /** Seed passport-related data from stored JSON. */
    PASSPORT_FROM_JSON,

    /** Seed both user and passport data(From JSON). */
    ALL
  }

  /**
   * Executes the seeding process for the given type.
   */
  @Command(
      command = "seed",
      description =
          """
            Seed database tables with initial or sample data.

            Available seed types:
              USER               - Seed only user data
              PASSPORT_FROM_API  - Seed passport data via API
              PASSPORT_FROM_JSON - Seed passport data from local JSON templates
              ALL (default)      - Seed users + passports (from JSON)

            Examples:
              seed --type USER
              seed --type PASSPORT_FROM_API
              seed --type PASSPORT_FROM_JSON
              seed --type ALL
              seed                 (defaults to ALL)
            """)
  public void seed(@Option(longNames = "type", defaultValue = "ALL") SeedType seedType) {
    log.info("Seeding started.");
    try {

      switch (seedType) {
        case USER -> userSeeder.seed();
        case PASSPORT_FROM_API -> passportFromApiSeeder.seed();
        case PASSPORT_FROM_JSON -> passportFromJsonSeeder.seed();
        case ALL -> {
          seedAll();
        }
        default -> {
          seedAll();
        }
      }

      log.info("Seeding complete.");
    } catch (RuntimeException e) {
      log.error("Seeding failed: {}", e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      log.error("Seeding failed: {}", e.getMessage(), e);
      throw new RuntimeException("Seeding failed", e);
    }
  }

  private void seedAll() {
    userSeeder.seed();
    passportFromJsonSeeder.seed();
  }
}
