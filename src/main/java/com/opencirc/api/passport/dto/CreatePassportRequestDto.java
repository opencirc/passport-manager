package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Template Model. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreatePassportRequestDto {

  /** Formed Template in JSON format. */
  @JsonProperty private JsonNode datasheetData;

  /** Data Category (Generic/Unique). */
  private String dataCategory;

  /** Name of the passport. */
  private String passportName;

  /** Parent Id of the passport. */
  private String parentId;

  /** ID of the user who created the passport. */
  private String createdById;

  /** User metadata who creates the passport. */
  private CreatedByDto createdBy;

  /** Time of creation. */
  private LocalDateTime createdTime;
}
