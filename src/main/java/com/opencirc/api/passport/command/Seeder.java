package com.opencirc.api.passport.command;

import com.opencirc.api.passport.exception.JsonValidationException;
import java.util.Arrays;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * Seeder command entry point for running seed operations. Supports seeding of the administrator,
 * passports, or both.
 */
@CommandGroup(name = "Seeder commands")
@Slf4j
@Component
public class Seeder {

  /** AdminSeeder bean. */
  private final AdminSeeder adminSeeder;

  /** Passport seeder via data dictionary API. */
  private final PassportFromApiSeeder passportFromApiSeeder;

  /** Passport seeder from stored JSON templates. */
  private final PassportFromJsonSeeder passportFromJsonSeeder;

  /** Constructor-based dependency injection for seeder components. */
  public Seeder(
      AdminSeeder adminSeeder,
      PassportFromApiSeeder passportFromApiSeeder,
      PassportFromJsonSeeder passportFromJsonSeeder) {
    this.adminSeeder = adminSeeder;
    this.passportFromApiSeeder = passportFromApiSeeder;
    this.passportFromJsonSeeder = passportFromJsonSeeder;
  }

  /** Enum representing supported seed types. */
  @Getter
  public enum SeedType {

    /** Seed only the administrator account. */
    ADMIN("admin"),

    /** Seed passport-related data from API. */
    PASSPORT_FROM_API("passportFromApi"),

    /** Seed passport-related data from stored JSON. */
    PASSPORT_FROM_JSON("passportFromJson"),

    /** Seed the administrator and passport data (from JSON). */
    ALL("all");

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
      name = "seed",
      description =
          """
            Seed database tables with initial or sample data.

            Available seed types:
              admin              - Seed only the administrator account
              passportFromApi    - Seed passport data via API
              passportFromJson   - Seed passport data from local JSON templates
              all (default)      - Seed the administrator + passports (from JSON)

            Examples:
              seed --type admin
              seed --type passportFromApi
              seed --type passportFromJson
              seed --type all
              seed                 (defaults to ALL)
            """)
  public void seed(@Option(longName = "type", defaultValue = "all") String seedTypeText) {
    log.info("Seeding started.");
    var seedType = SeedType.fromValue(seedTypeText);
    try {

      switch (seedType) {
        case ADMIN -> adminSeeder.seed();
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

  private void seedAll() throws JsonValidationException {
    adminSeeder.seed();
    passportFromJsonSeeder.seed();
  }
}
