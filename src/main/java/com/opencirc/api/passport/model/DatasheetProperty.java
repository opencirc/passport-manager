package com.opencirc.api.passport.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

/** Model for datasheet_properties table. */
@Entity
@Table(name = "datasheet_properties")
@Data
@ToString
public class DatasheetProperty {

  /** Unique Id for Datasheet. */
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  /** Datasheet id. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datasheet_id", referencedColumnName = "id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @ToString.Exclude
  @JsonBackReference
  private Datasheet datasheet;

  /** Code of the class. */
  @Column(name = "code")
  private String code;

  /** Uri of the platform. */
  @Column(name = "platform_id")
  private String platformId;

  /** PropertySet to which a property belongs. */
  @Column(name = "group_tag")
  private String groupTag;

  /** Type. */
  @Column(name = "property_type")
  private String propertyType;

  /** Definition. */
  @Column(name = "definition", columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private JsonNode definition;
}
