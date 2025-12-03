package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.DatasheetProperty;
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
   * Maps the DatasheetProperty values to dto.
   */
  public static DatasheetPropertyDto from(DatasheetProperty property) {
    DatasheetPropertyDto datasheetPropertyDto = new DatasheetPropertyDto();
    datasheetPropertyDto.setId(property.getId());
    datasheetPropertyDto.setDatasheetId(property.getDatasheet().getId());
    datasheetPropertyDto.setCode(property.getCode());
    datasheetPropertyDto.setPlatformId(property.getPlatformId());
    datasheetPropertyDto.setGroupTag(property.getGroupTag());
    datasheetPropertyDto.setPropertyType(property.getPropertyType());
    datasheetPropertyDto.setDefinition(property.getDefinition());
    return datasheetPropertyDto;
  }
}
