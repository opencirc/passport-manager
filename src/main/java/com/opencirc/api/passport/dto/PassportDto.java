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

  @JsonProperty private String projectId;

  @JsonProperty private String name;

  @JsonProperty private String status;

  @JsonProperty private String parentId;

  /** Id of the API key that created the passport; null for the root passport. */
  @JsonProperty private String createdById;

  @JsonProperty private OffsetDateTime createdTime;

  @JsonProperty private OffsetDateTime archivedTime;

  private List<DatasheetDto> datasheets;

  /** Setting up values from Passport to Passport Dto. */
  public static PassportDto from(Passport passport) {
    PassportDto dto = new PassportDto();
    dto.setId(passport.getId());
    dto.setProjectId(passport.getProjectId());
    dto.setName(passport.getName());
    dto.setStatus(passport.getStatus() != null ? passport.getStatus().getValue() : null);
    dto.setCreatedById(passport.getCreatedById());
    dto.setCreatedTime(passport.getCreatedTime());
    dto.setArchivedTime(passport.getArchivedTime());
    dto.setParentId(passport.getParentId());

    if (passport.getDatasheets() != null) {
      dto.setDatasheets(
          passport.getDatasheets().stream().map(DatasheetDto::from).collect(Collectors.toList()));
    }

    return dto;
  }
}
