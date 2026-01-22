package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** AddDatasheetsToPassportUsingPlatformRequestDto. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddDatasheetsToPassportUsingPlatformRequestDto {
  @NotBlank
  @JsonProperty
  private String platform;

  @NotBlank
  @JsonProperty
  private String platformId;

  @NotBlank
  @JsonProperty
  private String dataCategory;
}
