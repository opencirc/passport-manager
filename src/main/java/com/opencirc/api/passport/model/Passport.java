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

/** Model for Passports. */
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

  @Column(name = "name")
  private String name;

  @Column(name = "status")
  @Convert(converter = StatusConverter.class)
  private Status status;

  @Column(name = "parent_id")
  private String parentId;

  @Column(name = "created_by_id")
  private String createdById;

  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonManagedReference
  private Set<Datasheet> datasheets = new HashSet<>();

  /** Enum representing the status of a Passport. */
  @Getter
  public enum Status {
    ACTIVE("active"),

    INACTIVE("inactive");

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
