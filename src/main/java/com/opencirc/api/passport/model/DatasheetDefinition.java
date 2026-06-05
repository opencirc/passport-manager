package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.util.DataDictionaryConverter;
import com.opencirc.api.passport.util.DataDictionaryPlatformConverter;
import com.opencirc.api.passport.util.StringListJsonConverter;
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

/**
 * Model for a globally-shared datasheet definition. Sourced once from a data dictionary URI and
 * referenced by many passport datasheet instances rather than copied per passport.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "datasheet_definitions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class DatasheetDefinition {

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

  /** The dictionary URI; the natural, unique key for a definition. */
  @Column(name = "platform_id", nullable = false, unique = true)
  private String platformId;

  /** Related IFC entity URIs, used to expand into additional datasheets when requested. */
  @Column(name = "related_platform_ids", columnDefinition = "jsonb")
  @Convert(converter = StringListJsonConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private List<String> relatedPlatformIds;

  @Column(name = "synced_time", updatable = false, insertable = false)
  private OffsetDateTime syncedTime;

  @ToString.Exclude
  @JsonManagedReference("definition")
  @OneToMany(mappedBy = "datasheetDefinition", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private Set<DatasheetDefinitionProperty> properties = new HashSet<>();
}
