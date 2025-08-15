package com.opencirc.api.passport.command;

import java.util.Arrays;

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
     * PassportSeeder bean.
     */
    private final PassportSeederFromApi passportSeeder;

    /**
     * PassportSeeder bean.
     */
    private final PassportSeederFromJson passportJsonSeeder;

    /**
     * Constructor-based dependency injection for seeder components.
     *
     * @param userSeederParam
     * @param passportSeederParam
     */
    public Seeder(UserSeeder userSeederParam, PassportSeederFromApi passportSeederParam,
            PassportSeederFromJson passportJsonSeeder) {
        this.userSeeder = userSeederParam;
        this.passportSeeder = passportSeederParam;
        this.passportJsonSeeder = passportJsonSeeder;
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
     * Runs the seed process for the specified type.
     *
     * @param seedTypeInput
     * @throws RuntimeException
     */
    @Command(command = "seed", description = "Run seed")
    public void seed(
            @Option(longNames = "type", defaultValue = "all") String seedTypeInput,
            @Option(longNames = "stored-template", defaultValue = "true") boolean storedTemplateSource) {

        SeedType seedType;
        try {
            seedType = SeedType.valueOf(seedTypeInput.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid seed type '{}'. Valid types: {}. Default value is 'all'.",
                    seedTypeInput, Arrays.toString(SeedType.values()));
            throw e;
        }

        try {
            Runnable passportSeeding = () -> {
                if (storedTemplateSource) {
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
        } catch (Exception e) {
            log.error("Seeding failed: {}", e.getMessage(), e);
            throw e;
        }
    }

}
