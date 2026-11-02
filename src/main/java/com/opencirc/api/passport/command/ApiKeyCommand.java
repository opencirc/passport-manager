package com.opencirc.api.passport.command;

import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.service.ApiKeyService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/** Commands to issue, list and revoke machine API keys under access groups. */
@Component
@CommandGroup(name = "API Key commands")
@Slf4j
@RequiredArgsConstructor
public class ApiKeyCommand {

  private final ApiKeyService apiKeyService;

  private static final String API_KEY_ROW_FORMAT = "%-16s %-25s %-10s %-25s%n";

  /** Issues a new API key under the specified access group. */
  @Command(
      name = "create-api-key",
      description =
          """
            Issue a new API key under an access group. The token is printed once.
            Parameters:
              --accessGroupId   (required) ID of the access group
              --name            (required) name of the key
              --expirationDate  (optional) Expiration date in yyyy-MM-dd
            Example:
              create-api-key --accessGroupId <<access group id>> --name revit-pipeline
              --expirationDate 2027-12-31
            """)
  public void createApiKey(
      @Option(longName = "accessGroupId", required = true) String accessGroupId,
      @Option(longName = "name", required = true) String name,
      @Option(longName = "expirationDate") String expirationDate) {

    try {
      if (accessGroupId == null || accessGroupId.isBlank()) {
        throw new InvalidInputException("Access group ID is required.");
      }
      if (name == null || name.isBlank()) {
        throw new InvalidInputException("Name is required.");
      }

      ZonedDateTime expirationTime = null;
      if (expirationDate != null && !expirationDate.isBlank()) {
        LocalDate parsedDate;
        try {
          parsedDate = LocalDate.parse(expirationDate.trim());
        } catch (DateTimeParseException e) {
          throw new InvalidInputException(
              "Invalid expiration date " + "format. Expected yyyy-MM-dd.");
        }
        if (parsedDate.isBefore(LocalDate.now())) {
          throw new InvalidInputException("Expiration date cannot be in the past.");
        }
        expirationTime =
            parsedDate
                .atTime(LocalTime.MAX)
                .truncatedTo(ChronoUnit.MICROS)
                .atZone(ZoneId.systemDefault());
      }

      GeneratedApiKeyDto generated =
          apiKeyService.createApiKey(accessGroupId.trim(), name.trim(), expirationTime);

      System.out.println("API key created successfully");
      System.out.println("Key id: " + generated.getId());
      System.out.println("Token (shown once): " + generated.getToken());
      System.out.println("Use it as: Authorization: Bearer " + generated.getToken());

    } catch (InvalidInputException e) {
      log.warn("Validation error: {}", e.getMessage());
      System.err.println("Validation error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Error while creating API key: {}", e.getMessage(), e);
      System.err.println(e.getMessage());
    }
  }

  /** Lists the API keys issued by the specified access group. */
  @Command(
      name = "list-api-keys",
      description =
          """
            Lists the API keys issued by an access group.
            Parameters:
              --accessGroupId   (required) ID of the access group
            Example:
              list-api-keys --accessGroupId <<access group id>>
            """)
  public void listApiKeys(
      @Option(longName = "accessGroupId", required = true) String accessGroupId) {

    if (accessGroupId == null || accessGroupId.isBlank()) {
      log.warn("Validation error: Access group ID is required.");
      System.err.println("Validation error: Access group ID is required.");
      return;
    }

    try {
      List<ApiKey> keys = apiKeyService.listApiKeys(accessGroupId.trim());
      if (keys.isEmpty()) {
        System.out.println("No API keys found for access group: " + accessGroupId);
        return;
      }

      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      System.out.printf(API_KEY_ROW_FORMAT, "Key id", "Name", "Status", "Expiration");
      System.out.println(
          "---------------------------------------" + "------------------------------------");

      for (ApiKey key : keys) {
        String expiration =
            (key.getExpirationTime() == null)
                ? "Never"
                : key.getExpirationTime().format(dateTimeFormatter);
        System.out.printf(
            API_KEY_ROW_FORMAT,
            key.getId(),
            key.getName() == null ? "" : key.getName(),
            key.getStatus() == null ? "" : key.getStatus().getValue(),
            expiration);
      }
    } catch (Exception e) {
      log.error("Error while listing API keys: {}", e.getMessage(), e);
      System.err.println("Error: " + e.getMessage());
    }
  }

  /** Revokes the API key with the given id. */
  @Command(
      name = "revoke-api-key",
      description =
          """
            Revokes an API key by id. The key is kept for provenance but rejected from now on.
            Parameters:
              --key         (required) ID of the API key
            Example:
              revoke-api-key --key <<key id>>
            """)
  public void revokeApiKey(@Option(longName = "key", required = true) String keyId) {

    if (keyId == null || keyId.isBlank()) {
      log.warn("Validation error: Key ID is required.");
      System.err.println("Validation error: Key ID is required.");
      return;
    }

    try {
      ApiKey revoked = apiKeyService.revokeApiKey(keyId.trim());
      System.out.println("Revoked API key: " + revoked.getId());
    } catch (InvalidInputException e) {
      log.warn("Validation error: {}", e.getMessage());
      System.err.println("Validation error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Error while revoking API key: {}", e.getMessage(), e);
      System.err.println("Error: " + e.getMessage());
    }
  }
}
