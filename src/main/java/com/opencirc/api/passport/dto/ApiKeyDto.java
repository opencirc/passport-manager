package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.ApiKey;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API key DTO; never carries the secret or its hash. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDto {

  private String id;

  private String accessGroupId;

  private String name;

  private String status;

  private ZonedDateTime createdTime;

  private ZonedDateTime expirationTime;

  /** Maps an ApiKey to its DTO. */
  public static ApiKeyDto from(ApiKey apiKey) {
    return new ApiKeyDto(
        apiKey.getId(),
        apiKey.getAccessGroupId(),
        apiKey.getName(),
        apiKey.getStatus() != null ? apiKey.getStatus().getValue() : null,
        apiKey.getCreatedTime(),
        apiKey.getExpirationTime());
  }
}
