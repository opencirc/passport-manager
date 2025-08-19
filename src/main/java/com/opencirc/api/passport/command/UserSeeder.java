package com.opencirc.api.passport.command;

import org.springframework.stereotype.Component;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.config.AppProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Seeder for creating initial test users in the system.
 */
@Component
@Slf4j
public class UserSeeder {

    /**
     * Service for handling authentication and user registration.
     */
    private final AuthService authService;

    /**
     * Injecting Properties class.
     */
    private final AppProperties appProperties;

    /**
     * Constructor-based dependency injection for AuthService.
     *
     * @param authService
     * @param appProperties
     */
    public UserSeeder(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    /**
     * Seeds predefined test users into the system.
     *
     * @throws RuntimeException if user seeding fails.
     */
    public void seed() {

        registerSafely("admin@test.com", "test", "admin");
        registerSafely("user@test.com", "test", "user");
        log.info("User seeding completed.");

    }

    /**
     * Registers new user. Skips if user is already present.
     * @param email
     * @param firstName
     * @param lastName
     *
     */
    private void registerSafely(String email, String firstName, String lastName) {
        try {
            authService.register(email, appProperties.getDefaultSeedPassword(), firstName, lastName, null);
        } catch (Exception e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("exists")) {
                log.info("User '{}' already exists. Skipping.",
                        email);
                return;
            }
            throw e;
        }
    }

}
