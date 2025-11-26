package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
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
import org.hibernate.annotations.ColumnTransformer;

/** Model for passports table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "passports")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"datasheetMappings"})
public class Passport {

  /** Unique Id for Passport. */
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id")
  private String id;

  /** Name of Passport. */
  @Column(name = "name")
  private String name;

  /** Status of Passport. */
  @Column(name = "status")
  @Convert(converter = StatusConverter.class)
  private Status status;

  /** Id of Parent Passport. */
  @Column(name = "parent_id")
  private String parentId;

  /** Id of the user who created Passport. */
  @Column(name = "created_by_id")
  private String createdById;

  /** User information, stored as JSON. */
  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  /** Time of passport creation. */
  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  /** Mapping of Passport. */
  @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonManagedReference
  private Set<PassportDatasheetMapping> datasheetMappings = new HashSet<>();

  /** Enum representing the status of a Passport. */
  public enum Status {

    /** Indicates that the passport is active. */
    ACTIVE("active"),

    /** Indicates that the passport is inactive. */
    INACTIVE("inactive");

    /** Represents the status in string. */
    private final String value;

    /**
     * Constructs a Status enum with the specified string value.
     *
     * @param value the string representation of the status
     */
    Status(String value) {
      this.value = value;
    }

    /**
     * Gets the string value of the status.
     *
     * @return the status as a string
     */
    public String getValue() {
      return value;
    }

    /**
     * Returns the string representation of the status.
     *
     * @return the status as a string
     */
    @Override
    public String toString() {
      return value;
    }

    /**
     * Parses a string value to its corresponding enum.
     *
     * @param value the string value to convert
     * @return the corresponding Status enum
     * @throws IllegalArgumentException
     */
    public static Status fromValue(String value) {
      return Arrays.stream(Status.values())
          .filter(category -> category.value.equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid Status: " + value));
    }
  }
}
