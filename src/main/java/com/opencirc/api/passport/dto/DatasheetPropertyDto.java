package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.DatasheetProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Datasheet Property DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DatasheetPropertyDto {

  /** Unique Id for Datasheet Property. */
  @JsonProperty private String id;

  /** Code of the class. */
  @JsonProperty private String propertyCode;

  /** Id of the datasheet. */
  @JsonProperty private String datasheetId;

  /** Platform Id. */
  @JsonProperty private String platformId;

  /** Group of the property. */
  @JsonProperty private String propertyGroup;

  /** Type. */
  @JsonProperty private String propertyType;

  /** Property Definition. */
  @JsonProperty private JsonNode definition;

  /**
   * Maps the DatasheetProperty values to dto.
   *
   * @param property
   * @return datasheetPropertyDto
   */
  public static DatasheetPropertyDto from(DatasheetProperty property) {
    DatasheetPropertyDto datasheetPropertyDto = new DatasheetPropertyDto();
    datasheetPropertyDto.setId(property.getId());
    datasheetPropertyDto.setDatasheetId(property.getDatasheet().getId());
    datasheetPropertyDto.setPropertyCode(property.getPropertyCode());
    datasheetPropertyDto.setPlatformId(property.getPlatformId());
    datasheetPropertyDto.setPropertyGroup(property.getPropertyGroup());
    datasheetPropertyDto.setPropertyType(property.getPropertyType());
    datasheetPropertyDto.setDefinition(property.getDefinition());
    return datasheetPropertyDto;
  }
}
