package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.Project;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Project DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

  private String id;

  private String ownerId;

  private String name;

  private String description;

  private String status;

  /** Id of the root passport provisioned with the project. */
  private String rootPassportId;

  private String createdById;

  private CreatedByDto createdBy;

  private OffsetDateTime createdTime;

  private OffsetDateTime archivedTime;

  /** Maps a Project and the id of its root passport to the DTO. */
  public static ProjectDto from(Project project, String rootPassportId) {
    ProjectDto dto = new ProjectDto();
    dto.setId(project.getId());
    dto.setOwnerId(project.getOwnerId());
    dto.setName(project.getName());
    dto.setDescription(project.getDescription());
    dto.setStatus(project.getStatus() != null ? project.getStatus().getValue() : null);
    dto.setRootPassportId(rootPassportId);
    dto.setCreatedById(project.getCreatedById());
    dto.setCreatedBy(project.getCreatedBy());
    dto.setCreatedTime(project.getCreatedTime());
    dto.setArchivedTime(project.getArchivedTime());
    return dto;
  }
}
