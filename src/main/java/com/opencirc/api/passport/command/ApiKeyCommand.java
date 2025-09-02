package com.opencirc.api.passport.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.service.ApiKeyService;

import lombok.extern.slf4j.Slf4j;

@Command(group = "Create API key")
@Slf4j
public class ApiKeyCommand {

    /**
     * Injecting ApiKeyService.
     */
    private final ApiKeyService apiKeyService;

    /**
     * Constructor.
     * @param apiKeyService
     */
    public ApiKeyCommand(ApiKeyService apiKeyService) {
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

                } catch (DateTimeParseException e) {
                    throw new InvalidInputException(
                            "Invalid expiration date " + "format. Expected yyyy-MM-dd.");
                }

                if (formattedExpirationDate.isBefore(LocalDate.now())) {
                    throw new InvalidInputException(
                            "Expiration date cannot be in the past.");
                }
            }

            GeneratedApiKeyDto generatedApiKey = apiKeyService.createApiKey(userUuid,
                    formattedExpirationDate, name.trim());

            System.out.println("API Key created successfully");
            System.out.println("Key: " + generatedApiKey.getApiKey().getId());
            System.out.println("Secret: " + generatedApiKey.getRawSecret());

        } catch (InvalidInputException e) {
            System.out.println("Validation error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error while creating API key:");
            System.out.println("  Message: " + e.getMessage());
            System.out.println("  Type: " + e.getCause());
        }
    }


    /**
     * Lists all the available API token of the specified user.
     *
     * @param userId  the UUID of the user (required)
     */
    @Command(command = "list-api-tokens", description = """
            Retrieves all the api tokens associated with the user.
            Parameters:
              --user-id         (required) UUID of the user
            Example:
              list-api-tokens --user-id <<user id>>
            """)
    public void getApiTokens(
            @Option(longNames = "user-id", required = true) String userId) {

        if (userId == null || userId.isBlank()) {
            log.error("Validation error: User ID is required.");
            System.err.println("Validation error: User ID is required.");
            return;
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            System.err.println("Invalid UUID format: " + userId);
            return;
        }

        try {
            List<ApiKey> tokens = apiKeyService.getApiTokens(userUuid);
            if (tokens.isEmpty()) {
                System.out.println("No API tokens found for user: " + userId);
                return;
            }

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd");
            System.out.printf("%-40s %-20s %-25s%n", "Key", "Name", "Expiration");
            System.out.println("---------------------------------------"
                    + "------------------------------------");

            for (ApiKey token : tokens) {
                String expiration = (token.getExpirationTime() == null) ? "Never"
                        : token.getExpirationTime().format(dateTimeFormatter);
                System.out.printf("%-40s %-20s %-25s%n", token.getId(),
                        token.getName() == null ? "" : token.getName(), expiration);
            }
        } catch (Exception e) {
            log.error("Unexpected error while listing API keys: {}", e.getMessage(), e);
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Deletes the api token for the given key.
     * @param keyId
     */
    @Command(command = "delete-api-token", description = """
            Deletes an API token by ID.
            Parameters:
              --key         (required) UUID of the API key
            Example:
              delete-api-token --key <<key id>>
            """)
    public void deleteApiToken(
            @Option(longNames = "key", required = true) String keyId) {

        if (keyId == null || keyId.isBlank()) {
            log.error("Validation error: KeyId should be valid.");
            System.err.println("Validation error: KeyId should be valid.");
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(keyId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid key ID format: {}", keyId);
            System.err.println("Invalid UUID format: " + keyId);
            return;
        }

        try {
            boolean deleted = apiKeyService.deleteApiToken(uuid);
            if (deleted) {
                System.out.println("Deleted API token: " + keyId);
            } else {
                System.err.println("Key not found: " + keyId);
            }
        } catch (Exception e) {
            log.error("Unexpected error while deleting API key: {}", e.getMessage(), e);
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
