package com.opencirc.api.passport.command;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.model.User.Role;

import lombok.extern.slf4j.Slf4j;

@Command(group = "Register User Commands")
@Slf4j
public class RegisterUserCommand {

    /**
     * Injecting AuthService.
     */
    private final AuthService authService;

    /**
     * Constructor.
     * @param authService
     */
    public RegisterUserCommand(AuthService authService) {
        this.authService = authService;
    }


    /**
     * Shell command to register a new user.
     *
     * @param email
     * @param password
     * @param firstName
     * @param lastName
     * @param role
     * @return message
     */
    @Command(description = "Register a user.")
    public String register(@Option(longNames = "email", required = true) String email,
            @Option(longNames = "password", required = true) String password,
            @Option(longNames = "firstName", required = true) String firstName,
            @Option(longNames = "lastName", required = true) String lastName,
            @Option(longNames = "role", defaultValue = "user") String role) {

        Role parsedRole;
        try {
            parsedRole = (role == null) ? Role.USER : Role.fromValue(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return "Invalid role. Valid roles are 'user' or 'admin'.";
        }

        try {
            String userId = authService.register(email.trim(), password.trim(),
                    firstName.trim(), lastName.trim(), parsedRole);
            return String.format("User created with id: %s", userId);
        } catch (AuthenticationException authenticationException) {
            return "Registration failed: " + authenticationException.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error during registration for email={}", email, e);
            return "An unexpected error occurred during registration";
        }

    }

}
