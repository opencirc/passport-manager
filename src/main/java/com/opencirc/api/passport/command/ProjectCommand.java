package com.opencirc.api.passport.command;

import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dto.AccessGroupDto;
import com.opencirc.api.passport.dto.AccessGroupRequestDto;
import com.opencirc.api.passport.dto.CreateProjectRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.OwnerDto;
import com.opencirc.api.passport.dto.ProjectDto;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.service.AccessGroupService;
import com.opencirc.api.passport.service.OwnerService;
import com.opencirc.api.passport.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * Commands to provision owners, projects and access groups from the shell. Records created here are
 * attributed to the configured system administrator.
 */
@Component
@CommandGroup(name = "Project commands")
@RequiredArgsConstructor
public class ProjectCommand {

  private static final String PROJECT_ROW_FORMAT = "%-40s %-30s %-10s %-40s%n";

  private final OwnerService ownerService;

  private final ProjectService projectService;

  private final AccessGroupService accessGroupService;

  private final AppProperties appProperties;

  /** Creates an owner organization. */
  @Command(name = "create-owner", description = "Create an owner organization.")
  public String createOwner(@Option(longName = "name", required = true) String name) {
    OwnerDto owner = ownerService.createOwner(name);
    return String.format("Owner created with id: %s", owner.getId());
  }

  /** Creates a project together with its root passport. */
  @Command(
      name = "create-project",
      description = "Create a project; its root passport is provisioned in the same transaction.")
  public String createProject(
      @Option(longName = "name", required = true) String name,
      @Option(longName = "description") String description,
      @Option(longName = "ownerId") String ownerId) {
    ProjectDto project =
        projectService.createProject(
            new CreateProjectRequestDto(name, description, ownerId), systemAdmin(), null);
    return String.format(
        "Project created with id: %s (root passport: %s)",
        project.getId(), project.getRootPassportId());
  }

  /** Lists projects. */
  @Command(name = "list-projects", description = "List projects.")
  public String listProjects(
      @Option(longName = "includeArchived", defaultValue = "false") boolean includeArchived) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(PROJECT_ROW_FORMAT, "Id", "Name", "Status", "Root passport"));
    for (ProjectDto project : projectService.listProjects(includeArchived)) {
      sb.append(
          String.format(
              PROJECT_ROW_FORMAT,
              project.getId(),
              project.getName(),
              project.getStatus(),
              project.getRootPassportId()));
    }
    return sb.toString();
  }

  /** Soft-archives a project. */
  @Command(name = "archive-project", description = "Soft-archive a project and its passports.")
  public String archiveProject(@Option(longName = "projectId", required = true) String projectId) {
    ProjectDto project = projectService.archiveProject(projectId);
    return String.format("Project %s is now %s", project.getId(), project.getStatus());
  }

  /** Creates an access group with CRUD permissions in a project. */
  @Command(name = "create-access-group", description = "Create an access group in a project.")
  public String createAccessGroup(
      @Option(longName = "projectId", required = true) String projectId,
      @Option(longName = "name", required = true) String name,
      @Option(longName = "description") String description,
      @Option(longName = "canCreate", defaultValue = "true") boolean canCreate,
      @Option(longName = "canRead", defaultValue = "true") boolean canRead,
      @Option(longName = "canUpdate", defaultValue = "true") boolean canUpdate,
      @Option(longName = "canDelete", defaultValue = "false") boolean canDelete) {
    AccessGroupDto accessGroup =
        accessGroupService.createAccessGroup(
            projectId,
            new AccessGroupRequestDto(
                name,
                description,
                new AccessGroupPermissions(canCreate, canRead, canUpdate, canDelete)),
            systemAdmin(),
            null);
    return String.format("Access group created with id: %s", accessGroup.getId());
  }

  private CreatedByDto systemAdmin() {
    return new CreatedByDto(
        appProperties.getSystemAdminName(), appProperties.getSystemAdminEmail());
  }
}
