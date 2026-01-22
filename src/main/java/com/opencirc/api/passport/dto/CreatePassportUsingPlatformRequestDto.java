package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** CreatePassportUsingPlatformRequestDto. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreatePassportUsingPlatformRequestDto {
  @NotBlank
  @JsonProperty
  private String platformId;

  @NotBlank
  @JsonProperty
  private String name;

  @NotBlank
  @JsonProperty
  private String dataCategory;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty
  private String parentId;
}
