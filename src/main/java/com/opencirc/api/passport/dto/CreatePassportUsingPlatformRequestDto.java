package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty private String platformId;

  @JsonProperty private String name;

  @JsonProperty private String dataCategory;

  @JsonProperty private String parentId;
}
