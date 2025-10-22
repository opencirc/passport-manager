package com.opencirc.api.passport.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

/** DTO for Template table. */
@Entity
@Table(name = "passport_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PassportTemplate {

  /** Unique Id for Template. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Name of the Template. */
  @Column(name = "name")
  private String name;

  /** Template in JSON format. */
  @Column(name = "template", columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private JsonNode template;

  /** Id of the user who created the template. */
  @Column(name = "created_by_id")
  private String createdById;

  /** User information, stored as JSON. */
  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  /** Template created time. */
  @Column(name = "created_time", updatable = false, insertable = false)
  private LocalDateTime createdTime;
}
