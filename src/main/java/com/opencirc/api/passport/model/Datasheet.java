package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
import com.opencirc.api.passport.util.DataCategoryConverter;
import com.opencirc.api.passport.util.DataDictionaryConverter;
import com.opencirc.api.passport.util.DataDictionaryPlatformConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

/** Model for datasheet table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "datasheets")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Datasheet {

  /** Unique Id for Datasheet. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @EqualsAndHashCode.Include
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Name of the data dictionary platform. */
  @Column(name = "platform")
  @Convert(converter = DataDictionaryPlatformConverter.class)
  private DataDictionaryPlatform platform;

  /** Name of the data dictionary from which template is fetched. */
  @Column(name = "dictionary")
  @Convert(converter = DataDictionaryConverter.class)
  private DataDictionary dictionary;

  /** Code of the class. */
  @Column(name = "code")
  private String code;

  /** Name of the class. */
  @Column(name = "name")
  private String name;

  /** Description of the class. */
  @Column(name = "description")
  private String description;

  /** Uri of the platform. */
  @Column(name = "platform_id")
  private String platformId;

  /** Data category (Unique or Generic). */
  @Column(name = "data_category")
  @Convert(converter = DataCategoryConverter.class)
  private DataCategory dataCategory;

  /** Template information in JSON format. */
  @Column(name = "data", columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private JsonNode data;

  /** Id of the user who created the datasheet. */
  @Column(name = "created_by_id")
  private String createdById;

  /** User information, stored as JSON. */
  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  /** Time when datasheet is created. */
  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  /** Mapping to Passports. */
  @OneToMany(mappedBy = "datasheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @ToString.Exclude
  @JsonManagedReference("datasheet")
  private List<PassportDatasheetMapping> datasheetMappings;

  /** Mapping to DatasheetProperties. */
  @ToString.Exclude
  @JsonManagedReference
  @OneToMany(mappedBy = "datasheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private Set<DatasheetProperty> datasheetProperties = new HashSet<>();

  /** Enum representing the category of a data. */
  public enum DataCategory {

    /** Generic datasheet. */
    GENERIC("generic"),

    /** Unique datasheet. */
    UNIQUE("unique");

    /** Data category value in string. */
    private final String value;

    /**
     * Constructs a DataCategory enum with the specified string value.
     *
     * @param category the string representation of the category
     */
    DataCategory(String category) {
      this.value = category;
    }

    /**
     * Gets the string value of the data category.
     *
     * @return the data category as a string
     */
    public String getValue() {
      return value;
    }

    /**
     * Returns the string representation of the data category.
     *
     * @return the data category as a string
     */
    @Override
    public String toString() {
      return value;
    }

    /**
     * Parses a string value to its corresponding enum.
     *
     * @param value the string value to convert
     * @return the corresponding data category enum
     * @throws IllegalArgumentException
     */
    public static DataCategory fromValue(String value) {
      return Arrays.stream(DataCategory.values())
          .filter(category -> category.value.equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid category: " + value));
    }
  }
}
