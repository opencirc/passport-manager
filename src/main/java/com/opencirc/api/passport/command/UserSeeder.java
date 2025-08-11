package com.opencirc.api.passport.command;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.dto.RegisterUserDto;
import com.opencirc.api.passport.exception.AuthenticationException;

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
     * Constructor-based dependency injection for AuthService.
     *
     * @param authServiceParam
     */
    public UserSeeder(AuthService authServiceParam) {
        this.authService = authServiceParam;
    }

    /**
     * Seeds predefined test users into the system.
     *
     * @throws RuntimeException if user seeding fails.
     */
    public void seed() {
        try {
            registerSafely(createUser("admin@test.com", "admin"));
            registerSafely(createUser("user@test.com", "user"));
            log.info("User seeding completed.");
        } catch (Exception e) {
            log.error("User seeding failed: {}", e.getMessage(), e);
            throw new RuntimeException("User seeding failed", e);
        }
    }

    /**
     * Verifies and skips if user is already present.
     * @param registerUserDto
     *
     */
    private void registerSafely(RegisterUserDto registerUserDto) {
        try {
            authService.register(registerUserDto);
        } catch (AuthenticationException e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("exists")) {
                log.info("User '{}' already exists. Skipping.",
                        registerUserDto.getUsername());
            } else {
                log.error("Failed registering user '{}': {}",
                        registerUserDto.getUsername(),
                        e.getMessage(), e);
                throw new RuntimeException("User seeding failed", e);
            }
        } catch (Exception e) {
            log.error("Failed registering user '{}': {}", registerUserDto.getUsername(),
                    e.getMessage(), e);
            throw new RuntimeException("User seeding failed", e);
        }
    }

    /**
     * Creates a RegisterUserDto with default values.
     *
     * @param email
     * @param username
     * @return Populated RegisterUserDto instance.
     */
    private RegisterUserDto createUser(String email, String username) {
        RegisterUserDto user = new RegisterUserDto();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword("Password123!");
        user.setCreatedTime(LocalDateTime.now());
        return user;
    }
}
