package com.opencirc.api.passport.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

/** DTO for PassportLifecycle table. */
@Entity
@Table(name = "passport_lifecycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportLifecycle {

  /** Unique Id for Passport Lifecycle. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  /** Passport id. */
  @Column(name = "passport_id", nullable = false)
  private String passportId;

  /** Type of the event. */
  @Column(name = "event_type", nullable = false)
  private String eventType;

  /** Lifecycle data in JSON format. */
  @Column(name = "data", columnDefinition = "json", nullable = false)
  private JsonNode data;

  /** Created by. */
  @Column(name = "created_by", nullable = false)
  private String createdBy;

  /** Created time. */
  @Column(name = "created_time", updatable = false, insertable = false)
  private LocalDateTime createdTime;
}
