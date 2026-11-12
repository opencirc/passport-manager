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
 * Globally shared datasheet definition cached from an external data dictionary (bSDD, Lexicon).
 * Fetched once per classification URI and referenced by many passport datasheet instances instead
 * of being copied per passport.
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

  @Column(name = "code", columnDefinition = "TEXT")
  private String code;

  @Column(name = "name", columnDefinition = "TEXT")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** The classification URI on the external platform; the natural, unique key. */
  @Column(name = "platform_id", columnDefinition = "TEXT", nullable = false, unique = true)
  private String platformId;

  /** URIs of related classifications (e.g. related IFC entities) on the same platform. */
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
