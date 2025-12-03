package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencirc.api.passport.util.RoleConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** DTO for User table. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

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

  @Column(nullable = false)
  @Convert(converter = RoleConverter.class)
  private Role role;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "refresh_token")
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String refreshToken;

  @Column(name = "created_time", updatable = false, insertable = false)
  private LocalDateTime createdTime;

  public String getFullName() {
    return firstName + " " + lastName;
  }

  /** Enum representing the roles. */
  @Getter
  public enum Role {

    /** Administrator role. */
    ADMIN("admin"),

    /** Standard user role. */
    USER("user");

    private final String value;

    /** Constructs a Role enum with the specified string value. */
    Role(String value) {
      this.value = value;
    }

    /** Returns the string representation of the role. */
    @Override
    public String toString() {
      return value;
    }

    /** Parses a string value to its corresponding enum. */
    public static Role fromValue(String value) {
      return Arrays.stream(Role.values())
          .filter(role -> role.value.equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + value));
    }
  }
}
