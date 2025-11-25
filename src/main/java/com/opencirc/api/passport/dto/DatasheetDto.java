package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.Datasheet;
import java.time.LocalDateTime;
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

  /** Unique Id for Datasheet. */
  @JsonProperty private String id;

  /** Name of the data dictionary platform. */
  @JsonProperty private String platform;

  /** Name of the data dictionary. */
  @JsonProperty private String dictionary;

  /** Code of the class. */
  @JsonProperty private String code;

  /** Name of the class. */
  @JsonProperty private String name;

  /** Description of the class. */
  @JsonProperty private String description;

  /** Uri of the platform. */
  @JsonProperty private String platformId;

  /** Data category (Unique or Generic). */
  @JsonProperty private String dataCategory;

  /** Template information in JSON format. */
  @JsonProperty private JsonNode data;

  /** Id of the user who created the datasheet. */
  @JsonProperty private String createdById;

  /** Metadata of the user who created datasheet. */
  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private CreatedByDto createdBy;

  /** Time when datasheet is created. */
  @JsonProperty private LocalDateTime createdTime;

  /** Linked datasheet Properties. */
  private List<DatasheetPropertyDto> datasheetProperties;

  /**
   * Maps the Datasheet values to dto.
   *
   * @param datasheet
   * @return datasheetDto
   */
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
    datasheetDto.dataCategory =
        datasheet.getDataCategory() != null ? datasheet.getDataCategory().getValue() : null;
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
