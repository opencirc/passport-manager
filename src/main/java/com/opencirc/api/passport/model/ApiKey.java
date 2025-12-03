package com.opencirc.api.passport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** JPA entity representing API keys assigned to users. */
@Entity
@Table(
    name = "api_keys",
    indexes = {@Index(name = "idx_api_keys_user_id", columnList = "user_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

  /** Unique identifier for the API key. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private String id;

  /** Hashed secret associated with this API key. */
  @Column(nullable = false)
  private String secret;

  /** ID of the user who owns this API key, as UUID. */
  @Column(name = "user_id", nullable = false)
  private String userId;

  /** User-defined display name for the API key. */
  @Column(name = "name", length = 100, nullable = false)
  private String name;

  /** Timestamp when the API key record was created. */
  @Column(name = "created_time", nullable = false, updatable = false, insertable = false)
  private ZonedDateTime createdTime;

  /** Optional timestamp indicating when the API key expires. */
  @Column(name = "expiration_time")
  private ZonedDateTime expirationTime;
}
