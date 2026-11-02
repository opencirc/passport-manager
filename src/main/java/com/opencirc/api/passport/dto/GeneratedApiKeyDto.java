package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.ApiKey;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response to issuing an API key. The token is returned exactly once and never stored. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedApiKeyDto {

  private String id;

  private String accessGroupId;

  private String name;

  private String status;

  /** The full bearer token: {@code opc_live_<key_id>_<secret>}. */
  private String token;

  private ZonedDateTime createdTime;

  private ZonedDateTime expirationTime;

  /** Builds the response for a freshly issued key. */
  public static GeneratedApiKeyDto from(ApiKey apiKey, String token) {
    return new GeneratedApiKeyDto(
        apiKey.getId(),
        apiKey.getAccessGroupId(),
        apiKey.getName(),
        apiKey.getStatus() != null ? apiKey.getStatus().getValue() : null,
        token,
        apiKey.getCreatedTime(),
        apiKey.getExpirationTime());
  }
}
