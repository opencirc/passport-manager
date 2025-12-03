package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.Datasheet;
import java.time.OffsetDateTime;
import java.util.List;
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

  @JsonProperty private JsonNode data;

  @JsonProperty private String createdById;

  @JsonProperty private CreatedByDto createdBy;

  @JsonProperty private OffsetDateTime createdTime;

  private List<DatasheetPropertyDto> datasheetProperties;

  /** Maps the Datasheet values to dto. */
  public static DatasheetDto from(Datasheet datasheet) {
    DatasheetDto datasheetDto = new DatasheetDto();
    datasheetDto.id = datasheet.getId();
    datasheetDto.platform =
        datasheet.getPlatform() != null ? datasheet.getPlatform().getValue() : null;
    datasheetDto.dictionary =
        datasheet.getDictionary() != null ? datasheet.getDictionary().getValue() : null;
    datasheetDto.code = datasheet.getCode();
    datasheetDto.name = datasheet.getName();
    datasheetDto.description = datasheet.getDescription();
    datasheetDto.platformId = datasheet.getPlatformId();
    datasheetDto.dataCategory = datasheet.getDataCategory().getValue();
    datasheetDto.data = datasheet.getData();
    datasheetDto.createdById = datasheet.getCreatedById();
    datasheetDto.createdBy = datasheet.getCreatedBy();
    datasheetDto.createdTime = datasheet.getCreatedTime();

    if (datasheet.getDatasheetProperties() != null) {
      datasheetDto.datasheetProperties =
          datasheet.getDatasheetProperties().stream()
              .map(DatasheetPropertyDto::from)
              .collect(Collectors.toList());
    }
    return datasheetDto;
  }
}
