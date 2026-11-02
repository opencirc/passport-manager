package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.opencirc.api.passport.util.StatusConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Model for Passports. A root passport ({@code parentId == null}) is provisioned together with its
 * project; child passports are authored by machine credentials.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "passports")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"datasheets"})
public class Passport {

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id")
  private String id;

  /** Project the passport belongs to; every passport carries it for O(1) authorization. */
  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "name", columnDefinition = "TEXT")
  private String name;

  @Column(name = "status")
  @Convert(converter = StatusConverter.class)
  private Status status;

  @Column(name = "parent_id")
  private String parentId;

  /** Id of the API key that created the passport; null for the root passport. */
  @Column(name = "created_by_id")
  private String createdById;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  @Column(name = "archived_time")
  private OffsetDateTime archivedTime;

  @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonManagedReference
  private Set<Datasheet> datasheets = new HashSet<>();

  /** Lifecycle and archiving status of a passport. */
  @Getter
  public enum Status {
    /** Component is installed in the building assembly. */
    ACTIVE("active"),

    /** Component has been uninstalled, decommissioned, or staged for reuse. */
    INACTIVE("inactive"),

    /** Record is soft-deleted; hidden from default queries but preserved. */
    ARCHIVED("archived");

    private final String value;

    /** Constructs a Status enum with the specified string value. */
    Status(String value) {
      this.value = value;
    }

    /** Returns the string representation of the status. */
    @Override
    public String toString() {
      return value;
    }

    /** Parses a string value to its corresponding enum. */
    public static Status fromValue(String value) {
      return Arrays.stream(Status.values())
          .filter(category -> category.value.equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid Status: " + value));
    }
  }
}
