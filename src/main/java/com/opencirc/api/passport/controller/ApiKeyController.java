package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.dto.ApiKeyDto;
import com.opencirc.api.passport.dto.CreateApiKeyRequestDto;
import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrative endpoints for issuing and revoking machine API keys. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin: API Keys", description = "Machine credentials issued under access groups")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  /** Issues a key; the token is returned once and never stored. */
  @Operation(summary = "Issues an API key under an access group; the token is shown only once")
  @PostMapping("/access-groups/{groupId}/api-keys")
  public ResponseEntity<GeneratedApiKeyDto> createApiKey(
      @PathVariable String groupId, @Valid @RequestBody CreateApiKeyRequestDto request) {
    ZonedDateTime expiration =
        request.getExpirationTime() != null ? request.getExpirationTime().toZonedDateTime() : null;
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(apiKeyService.createApiKey(groupId, request.getName(), expiration));
  }

  /** Lists the keys of an access group. */
  @Operation(summary = "Lists the API keys of an access group")
  @GetMapping("/access-groups/{groupId}/api-keys")
  public ResponseEntity<List<ApiKeyDto>> listApiKeys(@PathVariable String groupId) {
    return ResponseEntity.ok(
        apiKeyService.listApiKeys(groupId).stream().map(ApiKeyDto::from).toList());
  }

  /** Revokes a key. */
  @Operation(summary = "Revokes an API key; it is rejected from now on but kept for provenance")
  @PostMapping("/api-keys/{id}/revoke")
  public ResponseEntity<ApiKeyDto> revokeApiKey(@PathVariable String id) {
    return ResponseEntity.ok(ApiKeyDto.from(apiKeyService.revokeApiKey(id)));
  }
}
