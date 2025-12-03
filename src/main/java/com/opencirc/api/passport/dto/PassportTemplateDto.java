package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.model.PassportTemplate;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Passport DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportTemplateDto {

  /** Unique Id. */
  @JsonProperty private String id;

  /** Name of the Template. */
  @JsonProperty private String name;

  /** Template in JSON format. */
  @JsonProperty private JsonNode template;

  /** Id of the user who created the template. */
  @JsonProperty private String createdById;

  /** Metadata of the user who created the template. */
  @JsonProperty private CreatedByDto createdBy;

  /** Time of creation. */
  @JsonProperty
  @Column(name = "created_time", updatable = false)
  private LocalDateTime createdTime;

  /**
   * Sets the passportTemplateDto from PassportTemplate.
   */
  public static PassportTemplateDto from(PassportTemplate passportTemplate) {
    PassportTemplateDto dto = new PassportTemplateDto();
    dto.setId(passportTemplate.getId());
    dto.setName(passportTemplate.getName());
    dto.setTemplate(passportTemplate.getTemplate());
    dto.setCreatedById(passportTemplate.getCreatedById());
    dto.setCreatedBy(passportTemplate.getCreatedBy());
    dto.setCreatedTime(passportTemplate.getCreatedTime());

    return dto;
  }
}
