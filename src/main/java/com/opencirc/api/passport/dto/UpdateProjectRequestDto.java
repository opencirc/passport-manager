package com.opencirc.api.passport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Full update of a project: name, description and ownership. A null owner clears the owner. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequestDto {

  /** New name of the project. */
  @NotBlank private String name;

  /** New description; null clears it. */
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  private String description;

  /** Id of the owner organization; null leaves the project without an owner. */
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  private String ownerId;
}
