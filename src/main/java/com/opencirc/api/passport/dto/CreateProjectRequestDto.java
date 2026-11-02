package com.opencirc.api.passport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for creating a project; its root passport is provisioned in the same transaction. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequestDto {

  @NotBlank private String name;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  private String description;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  private String ownerId;
}
