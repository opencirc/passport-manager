package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencirc.api.passport.model.Passport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Passport DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportDto {

  /** Unique Id. */
  @JsonProperty private String id;

  /** Name of Passport. */
  @JsonProperty private String name;

  /** Status of Passport. */
  @JsonProperty private String status;

  /** Id of Parent Passport. */
  @JsonProperty private String parentId;

  /** Id of the user who created Passport. */
  @JsonProperty private String createdById;

  /** Metadata of the user who created Passport. */
  @JsonProperty private CreatedByDto createdBy;

  /** Time of passport creation. */
  @JsonProperty private OffsetDateTime createdTime;

  /** Linked datasheets. */
  private List<DatasheetDto> datasheets;

  /**
   * Setting up values from Passport to Passport Dto.
   *
   * @param passport
   * @return passportDto
   */
  public static PassportDto from(Passport passport) {
    PassportDto dto = new PassportDto();
    dto.setId(passport.getId());
    dto.setName(passport.getName());
    dto.setStatus(passport.getStatus().getValue());
    dto.setCreatedById(passport.getCreatedById());
    dto.setCreatedBy(passport.getCreatedBy());
    dto.setCreatedTime(passport.getCreatedTime());
    dto.setParentId(passport.getParentId());

    if (passport.getDatasheetMappings() != null) {
      dto.setDatasheets(
          passport.getDatasheetMappings().stream()
              .map(mapping -> DatasheetDto.from(mapping.getDatasheet()))
              .collect(Collectors.toList()));
    }

    return dto;
  }
}
