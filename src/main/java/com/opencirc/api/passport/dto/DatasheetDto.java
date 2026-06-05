package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencirc.api.passport.model.Datasheet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Datasheet DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DatasheetDto {

  @JsonProperty private String id;

  @JsonProperty private String platform;

  @JsonProperty private String dictionary;

  @JsonProperty private String code;

  @JsonProperty private String name;

  @JsonProperty private String description;

  @JsonProperty private String platformId;

  @JsonProperty private String dataCategory;

  @JsonProperty private Map<String, Object> data;

  @JsonProperty private String createdById;

  @JsonProperty private CreatedByDto createdBy;

  @JsonProperty private OffsetDateTime createdTime;

  private List<DatasheetPropertyDto> datasheetProperties;

  /**
   * Maps a per-passport datasheet instance to a dto, pulling definition fields from the shared
   * {@link com.opencirc.api.passport.model.DatasheetDefinition} it references. The wire shape is
   * unchanged from when datasheets were copied per passport.
   */
  public static DatasheetDto from(Datasheet datasheet) {
    DatasheetDto datasheetDto = new DatasheetDto();
    datasheetDto.id = datasheet.getId();

    var definition = datasheet.getDefinition();
    if (definition != null) {
      datasheetDto.platform =
          definition.getPlatform() != null ? definition.getPlatform().getValue() : null;
      datasheetDto.dictionary =
          definition.getDictionary() != null ? definition.getDictionary().getValue() : null;
      datasheetDto.code = definition.getCode();
      datasheetDto.name = definition.getName();
      datasheetDto.description = definition.getDescription();
      datasheetDto.platformId = definition.getPlatformId();
    }

    datasheetDto.dataCategory =
        datasheet.getDataCategory() != null ? datasheet.getDataCategory().getValue() : null;
    datasheetDto.data = datasheet.getData();
    datasheetDto.createdById = datasheet.getCreatedById();
    datasheetDto.createdBy = datasheet.getCreatedBy();
    datasheetDto.createdTime = datasheet.getCreatedTime();

    if (definition != null && definition.getProperties() != null) {
      datasheetDto.datasheetProperties =
          definition.getProperties().stream()
              .map(property -> DatasheetPropertyDto.from(property, datasheet.getId()))
              .collect(Collectors.toList());
    }
    return datasheetDto;
  }
}
