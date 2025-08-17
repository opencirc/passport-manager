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
    private final PassportSeederFromApi passportSeeder;

    /**
     * Passport seeder from stored JSON templates.
     */
    private final PassportSeederFromJson passportJsonSeeder;

    /**
     * Constructor-based dependency injection for seeder components.
     *
     * @param userSeederParam
     * @param passportSeederParam
     * @param passportJsonSeederParam
     */
    public Seeder(UserSeeder userSeederParam, PassportSeederFromApi passportSeederParam,
            PassportSeederFromJson passportJsonSeederParam) {
        this.userSeeder = userSeederParam;
        this.passportSeeder = passportSeederParam;
        this.passportJsonSeeder = passportJsonSeederParam;
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
         * Seed only passport-related data.
         */
        PASSPORT,

        /**
         * Seed both user and passport data.
         */
        ALL
    }

    /**
     * Executes the seeding process for the given type.
     *
     * @param seedType       Seed type: USER | PASSPORT | ALL
     * @param storedTemplate - If true, seed passports from stored JSON templates;
     *                       otherwise, seed via the BSDD API.
     * @throws RuntimeException if seeding fails
     */
    @Command(command = "seed", description = "Run seed")
    public void seed(
            @Option(longNames = "type", defaultValue = "ALL") SeedType seedType,
            @Option(longNames = "stored-template", defaultValue = "true")
            boolean storedTemplate) {
        log.info("Seeding started.");
        try {
            Runnable passportSeeding = () -> {
                if (storedTemplate) {
                    passportJsonSeeder.seed();
                } else {
                    passportSeeder.seed();
                }
            };

            switch (seedType) {
            case USER -> userSeeder.seed();
            case PASSPORT -> passportSeeding.run();
            case ALL -> {
                userSeeder.seed();
                passportSeeding.run();
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
