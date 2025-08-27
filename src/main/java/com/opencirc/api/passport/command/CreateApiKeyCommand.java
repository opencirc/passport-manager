package com.opencirc.api.passport.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.service.ApiKeyService;

import lombok.extern.slf4j.Slf4j;

@Command(group = "Create API key")
@Slf4j
public class CreateApiKeyCommand {

    /**
     * Injecting ApiKeyService.
     */
    private final ApiKeyService apiKeyService;

    /**
     * Constructor.
     * @param apiKeyService
     */
    public CreateApiKeyCommand(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }


    /**
     * Creates a new API key for the specified user.
     *
     * @param userId         the UUID of the user (required)
     * @param expirationDate optional expiration date in yyyy-MM-dd format
     * @param name name of the token
     */
    @Command(command = "create-api-key", description = """
            Create a new API key for a user.
            Parameters:
              --user-id         (required) UUID of the user
              --expiration-date (optional) Expiration date in yyyy-MM-dd
              --name  name of the token
            Example:
              create-api-key --user-id <<user id>>
              --expiration-date 2025-12-31 --name test
            """)
    public void createApiKey(@Option(longNames = "user-id", required = true)
    String userId,
            @Option(longNames = "expiration-date") String expirationDate,
            @Option(longNames = "name", required = true) String name) {

        try {
            if (userId == null || userId.isEmpty()) {
                throw new InvalidInputException("User ID is required.");
            }

            if (name == null || name.isBlank()) {
                throw new InvalidInputException("Name is required.");
            }

            UUID userUuid;
            try {
                userUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Invalid user-id. Expected a UUID.");
            }

            LocalDate formattedExpirationDate = null;
            if (expirationDate != null && !expirationDate.isBlank()) {
                try {
                    formattedExpirationDate = LocalDate.parse(expirationDate);
                    if (formattedExpirationDate.isBefore(LocalDate.now())) {
                        throw new InvalidInputException(
                                "Expiration date cannot be in the past.");
                    }
                } catch (DateTimeParseException e) {
                    throw new InvalidInputException(
                            "Invalid expiration date " + "format. Expected yyyy-MM-dd.");
                }
            }

            final String trimmedName = name.trim();

            if (trimmedName.length() > 100) {
                throw new InvalidInputException("Name must be at most 100 characters.");
            }

            GeneratedApiKeyDto generatedApiKey = apiKeyService.createApiKey(userUuid,
                    formattedExpirationDate, trimmedName);

            log.info("API Key created successfully");
            log.info("Key: {}", generatedApiKey.getApiKey().getId());
            log.info("Secret: {}", generatedApiKey.getRawSecret());

        } catch (InvalidInputException e) {
            log.error("Validation error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while creating API key: {}", e.getMessage(), e);
        }
    }
}
