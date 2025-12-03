package com.opencirc.api.passport.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencirc.api.passport.exception.JsonValidationException;
import java.util.Arrays;
import lombok.Getter;
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

  /** Constructor-based dependency injection for seeder components. */
  public Seeder(
      UserSeeder userSeeder,
      PassportFromApiSeeder passportFromApiSeeder,
      PassportFromJsonSeeder passportFromJsonSeeder) {
    this.userSeeder = userSeeder;
    this.passportFromApiSeeder = passportFromApiSeeder;
    this.passportFromJsonSeeder = passportFromJsonSeeder;
  }

  /** Enum representing supported seed types. */
  @Getter
  public enum SeedType {

    /** Seed only user-related data. */
    USER("user"),

    /** Seed passport-related data from API. */
    PASSPORT_FROM_API("passportFromApi"),

    /** Seed passport-related data from stored JSON. */
    PASSPORT_FROM_JSON("passportFromJson"),

    /** Seed both user and passport data(From JSON). */
    ALL("all");

    /** Dictionary name in string. -- GETTER -- Gets the dictionary value. */
    private final String value;

    /** Constructor. */
    SeedType(String dictionaryValue) {
      this.value = dictionaryValue;
    }

    /** Returns the string representation of the enum. */
    @Override
    public String toString() {
      return value;
    }

    /** Parses a string value to its corresponding enum. */
    public static SeedType fromValue(String value) throws IllegalArgumentException {
      return Arrays.stream(SeedType.values())
          .filter(seedType -> seedType.value.equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid seed type: " + value));
    }
  }

  /** Executes the seeding process for the given type. */
  @Command(
      command = "seed",
      description =
          """
            Seed database tables with initial or sample data.

            Available seed types:
              user               - Seed only user data
              passportFromApi    - Seed passport data via API
              passportFromJson   - Seed passport data from local JSON templates
              all (default)      - Seed users + passports (from JSON)

            Examples:
              seed --type user
              seed --type passportFromApi
              seed --type passportFromJson
              seed --type all
              seed                 (defaults to ALL)
            """)
  public void seed(@Option(longNames = "type", defaultValue = "all") String seedTypeText) {
    log.info("Seeding started.");
    var seedType = SeedType.fromValue(seedTypeText);
    try {

      switch (seedType) {
        case USER -> userSeeder.seed();
        case PASSPORT_FROM_API -> passportFromApiSeeder.seed();
        case PASSPORT_FROM_JSON -> passportFromJsonSeeder.seed();
        default -> seedAll();
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

  private void seedAll() throws JsonValidationException, JsonProcessingException {
    userSeeder.seed();
    passportFromJsonSeeder.seed();
  }
}
