package com.opencirc.api.passport.model;

import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
import com.opencirc.api.passport.util.ProjectStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

/** Distinct physical building, construction development, or material workspace. */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "name", columnDefinition = "TEXT", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "status", nullable = false)
  @Convert(converter = ProjectStatusConverter.class)
  private ProjectStatus status;

  @Column(name = "created_by_id", columnDefinition = "TEXT")
  private String createdById;

  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;

  @Column(name = "archived_time")
  private OffsetDateTime archivedTime;
}
