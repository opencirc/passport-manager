package com.opencirc.api.passport.model;

import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.util.AccessGroupPermissionsConverter;
import com.opencirc.api.passport.util.CreatedByDtoConverter;
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

/** Policy profile within a project defining the CRUD permissions of its API keys. */
@Entity
@Table(name = "access_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessGroup {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "name", columnDefinition = "TEXT", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "permissions", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = AccessGroupPermissionsConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private AccessGroupPermissions permissions;

  @Column(name = "created_by_id", columnDefinition = "TEXT")
  private String createdById;

  @Column(name = "created_by", columnDefinition = "jsonb", nullable = false)
  @Convert(converter = CreatedByDtoConverter.class)
  @ColumnTransformer(write = "?::jsonb")
  private CreatedByDto createdBy;

  @Column(name = "created_time", updatable = false, insertable = false)
  private OffsetDateTime createdTime;
}
