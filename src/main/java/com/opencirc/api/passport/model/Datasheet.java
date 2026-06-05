package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
import com.opencirc.api.passport.util.DataCategoryConverter;
import com.opencirc.api.passport.util.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

/**
 * Per-passport datasheet instance. Links a passport to a globally-shared {@link
 * DatasheetDefinition} and holds only the passport-specific values.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "passport_datasheets")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passport", "definition"})
public class Datasheet {

  @Id
  @GeneratedValue
  @UuidGenerator
  @EqualsAndHashCode.Include
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Passport this datasheet instance belongs to. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "passport_id", referencedColumnName = "id", nullable = false)
  @JsonBackReference
  private Passport passport;

  /** Globally-shared definition this instance references. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", referencedColumnName = "id", nullable = false)
  private DatasheetDefinition definition;

  @Column(name = "data_category")
  @Convert(converter = DataCategoryConverter.class)
  private DataCategory dataCategory;

  @Column(name = "data", columnDefinition = "jsonb")
  @Convert(converter = JsonMapConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private Map<String, Object> data;

  @Column(name = "created_by_id")
  private String createdById;

  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  /** Enum representing the category of data. */
  @Getter
  public enum DataCategory {
    GENERIC("generic"),

    UNIQUE("unique");

    private final String value;

    /** Constructs a DataCategory enum with the specified string value. */
    DataCategory(String category) {
      this.value = category;
    }

    /** Returns the string representation of the data category. */
    @Override
    public String toString() {
      return value;
    }

    /** Parses a string value to its corresponding enum. */
    public static DataCategory fromValue(String value) {
      return Arrays.stream(DataCategory.values())
          .filter(category -> category.value.equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid category: " + value));
    }
  }
}
