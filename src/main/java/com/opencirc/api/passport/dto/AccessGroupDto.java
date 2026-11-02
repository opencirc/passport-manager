package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.AccessGroup;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Access group DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessGroupDto {

  private String id;

  private String projectId;

  private String name;

  private String description;

  private AccessGroupPermissions permissions;

  private String createdById;

  private CreatedByDto createdBy;

  private OffsetDateTime createdTime;

  /** Maps an AccessGroup to its DTO. */
  public static AccessGroupDto from(AccessGroup accessGroup) {
    return new AccessGroupDto(
        accessGroup.getId(),
        accessGroup.getProjectId(),
        accessGroup.getName(),
        accessGroup.getDescription(),
        accessGroup.getPermissions(),
        accessGroup.getCreatedById(),
        accessGroup.getCreatedBy(),
        accessGroup.getCreatedTime());
  }
}
