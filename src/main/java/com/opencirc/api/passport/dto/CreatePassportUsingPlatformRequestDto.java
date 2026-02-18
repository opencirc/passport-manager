package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.opencirc.api.passport.model.Datasheet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
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
  @NotBlank @JsonProperty private String platformId;

  @NotBlank @JsonProperty private String name;

  @Schema(
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "generic",
      allowableValues = {"generic", "unique"})
  @JsonProperty
  @JsonSetter(nulls = Nulls.SKIP)
  private String dataCategory = Datasheet.DataCategory.GENERIC.getValue();

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty
  private String parentId;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty
  private Map<String, Object> values;
}
