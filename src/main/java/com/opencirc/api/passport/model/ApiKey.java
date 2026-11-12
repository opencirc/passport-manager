package com.opencirc.api.passport.model;

import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.util.ApiKeyStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Machine credential issued to an access group. The id is the public key id embedded in the token
 * ({@code opc_live_<key_id>_<secret>}); only the SHA-256 hash of the secret is stored.
 */
@Entity
@Table(
    name = "api_keys",
    indexes = {@Index(name = "idx_api_keys_access_group_id", columnList = "access_group_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

  /** Public key id used for indexed lookup; assigned by the service, never generated. */
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Access group that issued this key. */
  @Column(name = "access_group_id", nullable = false)
  private String accessGroupId;

  /** Hex SHA-256 hash of the secret component of the token. */
  @Column(name = "secret_hash", columnDefinition = "TEXT", nullable = false)
  private String secretHash;

  /** Display name for the key. */
  @Column(name = "name", length = 100, nullable = false)
  private String name;

  /** Operational status; revoked keys are rejected but kept for provenance. */
  @Column(name = "status", nullable = false)
  @Convert(converter = ApiKeyStatusConverter.class)
  private ApiKeyStatus status;

  /** Timestamp when the key was created. */
  @Column(name = "created_time", nullable = false, updatable = false, insertable = false)
  private ZonedDateTime createdTime;

  /** Optional timestamp after which the key is rejected. */
  @Column(name = "expiration_time")
  private ZonedDateTime expirationTime;
}
