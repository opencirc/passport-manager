package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;
import tools.jackson.databind.JsonNode;

/** Property schema belonging to a globally shared datasheet definition. */
@Entity
@Table(name = "datasheet_definition_properties")
@Data
@ToString
public class DatasheetDefinitionProperty {

  /** Unique id of the property definition. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Datasheet definition this property belongs to. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", referencedColumnName = "id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @JsonBackReference("definition")
  private DatasheetDefinition datasheetDefinition;

  /** Standard property code. */
  @Column(name = "code", columnDefinition = "TEXT")
  private String code;

  /** Property URI on the external platform. */
  @Column(name = "platform_id", columnDefinition = "TEXT")
  private String platformId;

  /** Property set / group the property belongs to. */
  @Column(name = "group_tag", columnDefinition = "TEXT")
  private String groupTag;

  /** Data type specifier. */
  @Column(name = "property_type", columnDefinition = "TEXT")
  private String propertyType;

  /** Property schema detailing metadata, units, and constraints. */
  @Column(name = "definition", columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private JsonNode definition;
}
