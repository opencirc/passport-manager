package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
import com.opencirc.api.passport.util.DataCategoryConverter;
import com.opencirc.api.passport.util.DataDictionaryConverter;
import com.opencirc.api.passport.util.DataDictionaryPlatformConverter;
import com.opencirc.api.passport.util.JsonMapConverter;
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
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

/** Model for Datasheet. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "datasheets")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Datasheet {

  @Id
  @GeneratedValue
  @UuidGenerator
  @EqualsAndHashCode.Include
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "platform")
  @Convert(converter = DataDictionaryPlatformConverter.class)
  private Platform platform;

  @Column(name = "dictionary")
  @Convert(converter = DataDictionaryConverter.class)
  private DataDictionary dictionary;

  @Column(name = "code")
  private String code;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "platform_id")
  private String platformId;

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

  @OneToMany(mappedBy = "datasheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @ToString.Exclude
  @JsonManagedReference("datasheet")
  private List<PassportDatasheetMapping> datasheetMappings;

  @ToString.Exclude
  @JsonManagedReference
  @OneToMany(mappedBy = "datasheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private Set<DatasheetProperty> datasheetProperties = new HashSet<>();

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
