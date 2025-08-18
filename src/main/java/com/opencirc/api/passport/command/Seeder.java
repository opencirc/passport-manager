package com.opencirc.api.passport.command;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import lombok.extern.slf4j.Slf4j;

/**
 * Seeder command entry point for running seed operations. Supports seeding of
 * users, passports, or both.
 */
@Command(group = "Seeder Commands")
@Slf4j
public class Seeder {

    /**
     * UserSeeder bean.
     */
    private final UserSeeder userSeeder;

    /**
     * Passport seeder via data dictionary API.
     */
    private final PassportFromApiSeeder passportFromApiSeeder;

    /**
     * Passport seeder from stored JSON templates.
     */
    private final PassportFromJsonSeeder passportFromJsonSeeder;

    /**
     * Constructor-based dependency injection for seeder components.
     *
     * @param userSeederParam
     * @param passportFromApiSeeder
     * @param passportFromJsonSeeder
     */
    public Seeder(UserSeeder userSeederParam, PassportFromApiSeeder passportFromApiSeeder,
            PassportFromJsonSeeder passportFromJsonSeeder) {
        this.userSeeder = userSeederParam;
        this.passportFromApiSeeder = passportFromApiSeeder;
        this.passportFromJsonSeeder = passportFromJsonSeeder;
    }

    /**
     * Enum representing supported seed types.
     */
    private enum SeedType {

        /**
         * Seed only user-related data.
         */
        USER,

        /**
         * Seed passport-related data from API.
         */
        PASSPORT_FROM_API,

        /**
         * Seed passport-related data from Stored Json.
         */
        PASSPORT_FROM_JSON,

        /**
         * Seed both user and passport data(From Json).
         */
        ALL
    }

    /**
     * Executes the seeding process for the given type.
     *
     * @param seedType  Seed type: USER | PASSPORT_API | PASSPORT_JSON | ALL
     * @throws RuntimeException if seeding fails
     */
    @Command(command = "seed", description = "Run seed")
    public void seed(
            @Option(longNames = "type", defaultValue = "ALL") SeedType seedType) {
        log.info("Seeding started.");
        try {

            switch (seedType) {
            case USER -> userSeeder.seed();
            case PASSPORT_FROM_API -> passportFromApiSeeder.seed();
            case PASSPORT_FROM_JSON -> passportFromJsonSeeder.seed();
            case ALL -> {
                userSeeder.seed();
                passportFromJsonSeeder.seed();
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

}
