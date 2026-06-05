package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.DatasheetDefinitionProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DatasheetPropertyDto {

  @JsonProperty private String id;

  @JsonProperty private String code;

  @JsonProperty private String datasheetId;

  @JsonProperty private String platformId;

  @JsonProperty private String groupTag;

  @JsonProperty private String propertyType;

  @JsonProperty private JsonNode definition;

  /**
   * Maps a shared definition property to a dto. The owning per-passport datasheet instance id is
   * supplied so {@code datasheetId} keeps pointing at the instance, as before.
   */
  public static DatasheetPropertyDto from(
      DatasheetDefinitionProperty property, String datasheetInstanceId) {
    DatasheetPropertyDto datasheetPropertyDto = new DatasheetPropertyDto();
    datasheetPropertyDto.setId(property.getId());
    datasheetPropertyDto.setDatasheetId(datasheetInstanceId);
    datasheetPropertyDto.setCode(property.getCode());
    datasheetPropertyDto.setPlatformId(property.getPlatformId());
    datasheetPropertyDto.setGroupTag(property.getGroupTag());
    datasheetPropertyDto.setPropertyType(property.getPropertyType());
    datasheetPropertyDto.setDefinition(property.getDefinition());
    return datasheetPropertyDto;
  }
}
