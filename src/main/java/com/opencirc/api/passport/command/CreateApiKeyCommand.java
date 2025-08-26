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

@Command(group = "Create Api key Command")
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
     * @throws InvalidInputException if input validation fails
     */
    @Command(command = "create-api-key", description = """
            Create a new API key for a user.
            Parameters:
              --user-id         (required) UUID of the user
              --expiration-date (optional) Expiration date in yyyy-MM-dd
              --name (optional) name of the token
            Example:
              create-api-key --user-id <<user id>>
              --expiration-date 2025-12-31 --name test
            """)
    public void createApiKey(@Option(longNames = "user-id") String userId,
            @Option(longNames = "expiration-date") String expirationDate,
            @Option(longNames = "name") String name) {

        try {
            if (userId == null || userId.isEmpty()) {
                throw new InvalidInputException("User ID is required.");
            }

            UUID userUuid;
            try {
                userUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Invalid UUID format: " + userId);
            }

            LocalDate formattedExpirationDate = null;
            if (expirationDate != null && !expirationDate.isBlank()) {
                try {
                    formattedExpirationDate = LocalDate.parse(expirationDate);
                } catch (DateTimeParseException e) {
                    throw new InvalidInputException("Invalid expiration date "
                            + "format. Expected yyyy-MM-dd.");
                }
            }

            GeneratedApiKeyDto generatedApiKey = apiKeyService
                    .createApiKey(userUuid, formattedExpirationDate, name);

            log.info("API Key created successfully!");
            log.info("ID: {}", generatedApiKey.getApiKey().getId());
            log.info("Secret API Key: {}", generatedApiKey.getRawSecret());

        } catch (InvalidInputException e) {
            log.error("Validation error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while creating API key: {}", e.getMessage(), e);
        }
    }
}
