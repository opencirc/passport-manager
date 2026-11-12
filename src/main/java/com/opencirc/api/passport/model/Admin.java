package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Human system administrator. Administrators govern owners, projects, access groups and API keys;
 * every account in this table carries the same, full administrative authority.
 */
@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "email", unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "refresh_token")
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String refreshToken;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  public String getFullName() {
    return firstName + " " + lastName;
  }
}
