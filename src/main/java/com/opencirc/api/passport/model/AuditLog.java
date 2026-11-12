package com.opencirc.api.passport.model;

import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.util.AuditLogActionConverter;
import com.opencirc.api.passport.util.AuditTargetTypeConverter;
import com.opencirc.api.passport.util.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;
import tools.jackson.databind.JsonNode;

/**
 * Immutable record of a mutation applied to a passport, a passport datasheet or a datasheet
 * definition. The delta is stored as an RFC 6902 JSON Patch array; project and passport ids are
 * denormalised for zero-join feeds.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuditLog {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "target_type", nullable = false)
  @Convert(converter = AuditTargetTypeConverter.class)
  private AuditTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private String targetId;

  /** Project context; null for entries about the global definition catalogue. */
  @Column(name = "project_id")
  private String projectId;

  /** Passport context; null for entries about the global definition catalogue. */
  @Column(name = "passport_id")
  private String passportId;

  @Column(name = "action", nullable = false)
  @Convert(converter = AuditLogActionConverter.class)
  private AuditLogAction action;

  /** RFC 6902 JSON Patch operations from the previous to the new state. */
  @Column(name = "diff", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = JsonNodeConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private JsonNode diff;

  /** API key that performed the mutation; null for administrative provisioning. */
  @Column(name = "created_by_id")
  private String createdById;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;
}
