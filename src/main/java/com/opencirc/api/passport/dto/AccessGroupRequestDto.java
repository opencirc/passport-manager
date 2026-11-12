package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.AccessGroupPermissions;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for creating or updating an access group. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessGroupRequestDto {

  @NotBlank private String name;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  private String description;

  @NotNull private AccessGroupPermissions permissions;
}
