package com.opencirc.api.passport.service;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.AccessGroupRepository;
import com.opencirc.api.passport.dao.ApiKeyRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.exception.AuthenticationException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.AccessGroup;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.model.Project;
import com.opencirc.api.passport.util.SecretGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, revokes and authenticates machine credentials. Tokens have the form {@code
 * opc_live_<key_id>_<secret>}: the key id gives an O(1) indexed lookup and the secret is verified
 * against its SHA-256 hash in constant time.
 */
@Service
@Slf4j
public class ApiKeyService {

  private static final Pattern TOKEN_COMPONENT = Pattern.compile("[A-Za-z0-9]+");

  /** Hash compared against when the key id is unknown, so lookups fail in uniform time. */
  private static final String UNKNOWN_KEY_HASH = hashSecret("");

  private final ApiKeyRepository apiKeyRepository;

  private final AccessGroupRepository accessGroupRepository;

  private final ProjectRepository projectRepository;

  /** Constructor. */
  public ApiKeyService(
      ApiKeyRepository apiKeyRepository,
      AccessGroupRepository accessGroupRepository,
      ProjectRepository projectRepository) {
    this.apiKeyRepository = apiKeyRepository;
    this.accessGroupRepository = accessGroupRepository;
    this.projectRepository = projectRepository;
  }

  /** The key id and secret parsed out of a token. */
  public record ParsedToken(String keyId, String secret) {}

  /** Issues a new key under the access group and returns the token, which is shown only once. */
  @Transactional
  public GeneratedApiKeyDto createApiKey(
      String accessGroupId, String name, ZonedDateTime expirationTime) {
    if (accessGroupId == null || accessGroupId.isBlank()) {
      throw new InvalidInputException("Access group ID must not be null.");
    }
    if (!accessGroupRepository.existsById(accessGroupId)) {
      throw new InvalidInputException("No access group found with id: " + accessGroupId);
    }
    if (name == null || name.isBlank()) {
      throw new InvalidInputException("Name is required.");
    }
    if (name.length() > AppConstants.API_KEY_NAME_MAX_LENGTH) {
      throw new InvalidInputException(
          "Name must be at most " + AppConstants.API_KEY_NAME_MAX_LENGTH + " characters.");
    }
    if (expirationTime != null && !expirationTime.toInstant().isAfter(Instant.now())) {
      throw new InvalidInputException("Expiration time cannot be in the past");
    }

    String keyId = generateUniqueKeyId();
    String secret = SecretGenerator.generateRandomString(AppConstants.API_KEY_SECRET_LENGTH);

    ApiKey apiKey = new ApiKey();
    apiKey.setId(keyId);
    apiKey.setAccessGroupId(accessGroupId);
    apiKey.setSecretHash(hashSecret(secret));
    apiKey.setName(name.trim());
    apiKey.setStatus(ApiKeyStatus.ACTIVE);
    apiKey.setExpirationTime(expirationTime);
    apiKey = apiKeyRepository.save(apiKey);

    return GeneratedApiKeyDto.from(apiKey, buildToken(keyId, secret));
  }

  /** Lists the keys issued by an access group. */
  @Transactional(readOnly = true)
  public List<ApiKey> listApiKeys(String accessGroupId) {
    if (accessGroupId == null || accessGroupId.isBlank()) {
      throw new InvalidInputException("Access group ID must not be null");
    }
    if (!accessGroupRepository.existsById(accessGroupId)) {
      throw new InvalidInputException("No access group found with id: " + accessGroupId);
    }
    return apiKeyRepository.findAllByAccessGroupIdOrderByCreatedTimeAsc(accessGroupId);
  }

  /** Soft-revokes a key: it is rejected from now on but kept for provenance. */
  @Transactional
  public ApiKey revokeApiKey(String keyId) {
    if (keyId == null || keyId.isBlank()) {
      throw new InvalidInputException("Key ID must not be null");
    }
    ApiKey apiKey =
        apiKeyRepository
            .findById(keyId)
            .orElseThrow(() -> new InvalidInputException("No API key found with id: " + keyId));
    apiKey.setStatus(ApiKeyStatus.REVOKED);
    return apiKeyRepository.save(apiKey);
  }

  /** Fetches an API key by ID, returning null if not found. */
  public ApiKey findById(String id) {
    return apiKeyRepository.findById(id).orElse(null);
  }

  /**
   * Authenticates a bearer token. Fails with an {@link AuthenticationException} when the token is
   * malformed, unknown, has a wrong secret, is revoked or expired, or its project is archived.
   */
  @Transactional(readOnly = true)
  public ApiKeyPrincipal authenticate(String token) {
    ParsedToken parsed =
        parseToken(token)
            .orElseThrow(() -> new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS));

    ApiKey apiKey = apiKeyRepository.findById(parsed.keyId()).orElse(null);
    String expectedHash = apiKey != null ? apiKey.getSecretHash() : UNKNOWN_KEY_HASH;
    boolean secretMatches =
        MessageDigest.isEqual(
            expectedHash.getBytes(StandardCharsets.UTF_8),
            hashSecret(parsed.secret()).getBytes(StandardCharsets.UTF_8));
    if (apiKey == null || !secretMatches) {
      throw new AuthenticationException(AppConstants.ERR_INVALID_CREDENTIALS);
    }
    if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
      throw new AuthenticationException("API key revoked");
    }
    if (apiKey.getExpirationTime() != null
        && !Instant.now().isBefore(apiKey.getExpirationTime().toInstant())) {
      throw new AuthenticationException("API key expired");
    }
    return toPrincipal(apiKey);
  }

  /**
   * Builds the machine actor for an active key without its secret. Used by operator tooling (shell
   * commands, seeders) that runs with database access rather than a token.
   */
  @Transactional(readOnly = true)
  public ApiKeyPrincipal actorFor(String keyId) {
    ApiKey apiKey =
        apiKeyRepository
            .findById(keyId)
            .orElseThrow(() -> new InvalidInputException("No API key found with id: " + keyId));
    if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
      throw new InvalidInputException("API key is revoked: " + keyId);
    }
    return toPrincipal(apiKey);
  }

  private ApiKeyPrincipal toPrincipal(ApiKey apiKey) {
    AccessGroup accessGroup =
        accessGroupRepository
            .findById(apiKey.getAccessGroupId())
            .orElseThrow(() -> new AuthenticationException("API key access group not found"));
    Project project =
        projectRepository
            .findById(accessGroup.getProjectId())
            .orElseThrow(() -> new AuthenticationException("API key project not found"));
    if (project.getStatus() != ProjectStatus.ACTIVE) {
      throw new AuthenticationException("Project is archived");
    }
    return new ApiKeyPrincipal(
        apiKey.getId(),
        apiKey.getName(),
        accessGroup.getId(),
        project.getId(),
        accessGroup.getPermissions());
  }

  /**
   * Extracts the machine token from a request: {@code Authorization: Bearer opc_live_...} or the
   * {@code X-Api-Key} header. Returns null when neither is present.
   */
  public static String resolveToken(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null) {
      String trimmed = authorization.trim();
      if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
        String candidate = trimmed.substring("bearer ".length()).trim();
        if (candidate.startsWith(AppConstants.API_KEY_PREFIX)) {
          return candidate;
        }
      }
    }
    String header = request.getHeader(AppConstants.HEADER_API_KEY);
    if (header != null && !header.isBlank()) {
      return header.trim();
    }
    return null;
  }

  /** Splits a token into its key id and secret; empty when the format is wrong. */
  public static Optional<ParsedToken> parseToken(String token) {
    if (token == null) {
      return Optional.empty();
    }
    String trimmed = token.trim();
    if (!trimmed.startsWith(AppConstants.API_KEY_PREFIX)) {
      return Optional.empty();
    }
    String rest = trimmed.substring(AppConstants.API_KEY_PREFIX.length());
    int separator = rest.indexOf('_');
    if (separator <= 0 || separator == rest.length() - 1) {
      return Optional.empty();
    }
    String keyId = rest.substring(0, separator);
    String secret = rest.substring(separator + 1);
    if (!TOKEN_COMPONENT.matcher(keyId).matches() || !TOKEN_COMPONENT.matcher(secret).matches()) {
      return Optional.empty();
    }
    return Optional.of(new ParsedToken(keyId, secret));
  }

  /** Assembles a token from its components. */
  public static String buildToken(String keyId, String secret) {
    return AppConstants.API_KEY_PREFIX + keyId + "_" + secret;
  }

  /** Hex SHA-256 of a secret. */
  public static String hashSecret(String secret) {
    try {
      MessageDigest digest = MessageDigest.getInstance(AppConstants.SHA_256);
      return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private String generateUniqueKeyId() {
    for (int attempt = 0; attempt < 10; attempt++) {
      String keyId = SecretGenerator.generateRandomString(AppConstants.API_KEY_ID_LENGTH);
      if (!apiKeyRepository.existsById(keyId)) {
        return keyId;
      }
    }
    throw new IllegalStateException("Could not generate a unique API key id");
  }
}
