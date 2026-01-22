package com.opencirc.api.passport.command;

import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.service.ApiKeyService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
@Command(group = "API Key commands")
@Slf4j
@RequiredArgsConstructor
public class ApiKeyCommand {

  /** Injecting ApiKeyService. */
  private final ApiKeyService apiKeyService;

  /** Format to show API keys to user in terminal. */
  private static final String API_TOKEN_ROW_FORMAT = "%-40s %-20s %-25s%n";

  /** Creates a new API key for the specified user. */
  @Command(
      command = "create-api-key",
      description =
          """
            Create a new API key for a user.
            Parameters:
              --userId         (required) ID of the user
              --expiration-date (optional) Expiration date in yyyy-MM-dd
              --name  name of the token
            Example:
              create-api-key --userId <<user id>>
              --expiration-date 2025-12-31 --name test
            """)
  public void createApiKey(
      @Option(longNames = "userId", required = true) String userId,
      @Option(longNames = "expirationDate") String expirationDate,
      @Option(longNames = "name", required = true) String name) {

    try {
      if (userId == null || userId.isEmpty()) {
        throw new InvalidInputException("User ID is required.");
      }

      if (name == null || name.isBlank()) {
        throw new InvalidInputException("Name is required.");
      }

      LocalDate formattedExpirationDate = null;
      if (expirationDate != null && !expirationDate.isBlank()) {
        try {
          formattedExpirationDate = LocalDate.parse(expirationDate.trim());

        } catch (DateTimeParseException e) {
          throw new InvalidInputException(
              "Invalid expiration date " + "format. Expected yyyy-MM-dd.");
        }

        if (formattedExpirationDate.isBefore(LocalDate.now())) {
          throw new InvalidInputException("Expiration date cannot be in the past.");
        }
      }

      GeneratedApiKeyDto generatedApiKey =
          apiKeyService.createApiKey(userId.trim(), formattedExpirationDate, name.trim());

      System.out.println("API Key created successfully");
      System.out.println("X-Api-Key: " + generatedApiKey.getApiKey().getId());
      System.out.println("X-Api-Secret: " + generatedApiKey.getRawSecret());

    } catch (InvalidInputException e) {
      log.warn("Validation error: {}", e.getMessage());
      System.err.println("Validation error: " + e.getMessage());
      return;
    } catch (Exception e) {
      log.error("Error while creating API key: {}", e.getMessage(), e);
      System.err.println(e.getMessage());
    }
  }

  /** Lists all available API tokens for the specified user. */
  @Command(
      command = "list-api-tokens",
      description =
          """
            Retrieves all the API tokens associated with the user.
            Parameters:
              --userId         (required) ID of the user
            Example:
              list-api-tokens --userId <<user id>>
            """)
  public void getApiTokens(@Option(longNames = "userId", required = true) String userId) {

    if (userId == null || userId.isBlank()) {
      log.warn("Validation error: User ID is required.");
      System.err.println("Validation error: User ID is required.");
      return;
    }

    try {
      List<ApiKey> tokens = apiKeyService.getApiTokens(userId.trim());
      if (tokens.isEmpty()) {
        System.out.println("No API tokens found for user: " + userId);
        return;
      }

      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      System.out.printf(API_TOKEN_ROW_FORMAT, "Key", "Name", "Expiration");
      System.out.println(
          "---------------------------------------" + "------------------------------------");

      for (ApiKey token : tokens) {
        String expiration =
            (token.getExpirationTime() == null)
                ? "Never"
                : token.getExpirationTime().format(dateTimeFormatter);
        System.out.printf(
            API_TOKEN_ROW_FORMAT,
            token.getId(),
            token.getName() == null ? "" : token.getName(),
            expiration);
      }
    } catch (Exception e) {
      log.error("Error while listing API keys: {}", e.getMessage(), e);
      System.err.println("Error: " + e.getMessage());
    }
  }

  /** Deletes the api token for the given key. */
  @Command(
      command = "delete-api-token",
      description =
          """
            Deletes an API token by ID.
            Parameters:
              --key         (required) ID of the API key
            Example:
              delete-api-token --key <<key id>>
            """)
  public void deleteApiToken(@Option(longNames = "key", required = true) String keyId) {

    if (keyId == null || keyId.isBlank()) {
      log.warn("Validation error: Key ID is required.");
      System.err.println("Validation error: Key ID is required.");
      return;
    }

    try {
      boolean deleted = apiKeyService.deleteApiToken(keyId);
      if (deleted) {
        System.out.println("Deleted API token: " + keyId);
      } else {
        System.err.println("Key not found: " + keyId);
      }
    } catch (InvalidInputException e) {
      log.warn("Validation error: {}", e.getMessage());
      System.err.println("Validation error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Error while deleting API key: {}", e.getMessage(), e);
      System.err.println("Error: " + e.getMessage());
    }
  }
}
