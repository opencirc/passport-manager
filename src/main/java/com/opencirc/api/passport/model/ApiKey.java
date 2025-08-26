package com.opencirc.api.passport.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * JPA entity representing API keys assigned to users.
 */
@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_user_id", columnList = "user_id") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApiKey {

    /** Unique identifier for the API key. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /** Hashed secret associated with this API key. */
    @Column(nullable = false)
    private String secret;

    /** UUID of the user who owns this API key.  */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Optional user-defined display name for the API key. */
    @Column(name = "name")
    private String name;

    /** Timestamp when the API key record was created. */
    @Column(name = "created_time", nullable = false,
            updatable = false, insertable = false)
    private Instant createdTime;

    /** Optional timestamp indicating when the API key expires.  */
    @Column(name = "expiration_time")
    private Instant expirationTime;

}
