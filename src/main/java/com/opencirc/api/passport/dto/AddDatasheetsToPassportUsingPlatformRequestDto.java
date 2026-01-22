package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty private String platform;

  @JsonProperty private String platformId;

  @JsonProperty private String dataCategory;
}
