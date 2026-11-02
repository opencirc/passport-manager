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

  @JsonProperty private String id;

  @JsonProperty private String name;

  @JsonProperty private String status;

  @JsonProperty private String parentId;

  @JsonProperty private String createdById;

  @JsonProperty private CreatedByDto createdBy;

  @JsonProperty private OffsetDateTime createdTime;

  private List<DatasheetDto> datasheets;

  /** Setting up values from Passport to Passport Dto. */
  public static PassportDto from(Passport passport) {
    PassportDto dto = new PassportDto();
    dto.setId(passport.getId());
    dto.setName(passport.getName());
    dto.setStatus(passport.getStatus().getValue());
    dto.setCreatedById(passport.getCreatedById());
    dto.setCreatedBy(passport.getCreatedBy());
    dto.setCreatedTime(passport.getCreatedTime());
    dto.setParentId(passport.getParentId());

    if (passport.getDatasheets() != null) {
      dto.setDatasheets(
          passport.getDatasheets().stream().map(DatasheetDto::from).collect(Collectors.toList()));
    }

    return dto;
  }
}
