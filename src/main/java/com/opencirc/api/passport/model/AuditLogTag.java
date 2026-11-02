package com.opencirc.api.passport.model;

import com.opencirc.api.passport.enums.ValidationStatus;
import com.opencirc.api.passport.util.ValidationStatusConverter;
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
import org.hibernate.annotations.UuidGenerator;

/**
 * Signed validation state attached to one audit log entry. The signature is an HMAC-SHA256 over
 * {@code log_id:target_id:email:tag:sha256(diff)} keyed by the secret of the signing API key.
 */
@Entity
@Table(name = "audit_log_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuditLogTag {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** The audit log entry; at most one tag per entry. */
  @Column(name = "log_id", nullable = false, unique = true)
  private String logId;

  @Column(name = "tag", nullable = false)
  @Convert(converter = ValidationStatusConverter.class)
  private ValidationStatus tag;

  /** Email of the human validator. */
  @Column(name = "email", columnDefinition = "TEXT", nullable = false)
  private String email;

  /** API key whose secret signed the tag. */
  @Column(name = "api_key_id", nullable = false)
  private String apiKeyId;

  /** Hex HMAC-SHA256 signature. */
  @Column(name = "signature", columnDefinition = "TEXT", nullable = false)
  private String signature;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  @Column(name = "updated_time")
  private OffsetDateTime updatedTime;
}
