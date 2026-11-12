package com.opencirc.api.passport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for issuing an API key under an access group. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequestDto {

  @NotBlank private String name;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  private OffsetDateTime expirationTime;
}
