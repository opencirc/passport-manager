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
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

/** DTO for Logs table. */
@Entity
@Table(name = "passport_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportLog {

  /** Unique Id for logs. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Passport id. */
  @Column(name = "passport_id", nullable = false)
  private String passportId;

  /** Log information in JSON format. */
  @Column(name = "data", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = com.opencirc.api.passport.util.JsonNodeConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private JsonNode data;

  /** Created by. */
  @Column(name = "created_by_id")
  private String createdById;

  /** User information, stored as JSON. */
  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  /** Created time. */
  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;
}
