package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

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
