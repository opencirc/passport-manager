package com.opencirc.api.passport.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import tools.jackson.databind.JsonNode;

/** Public representation of a passport audit event. */
@Data
@AllArgsConstructor
public class PassportLogDto {
  public String id;
  public String passportId;
  public JsonNode data;
  public String createdById;
  public CreatedByDto createdBy;
  public OffsetDateTime createdTime;
}
