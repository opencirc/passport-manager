package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dto.AdminDto;
import com.opencirc.api.passport.dto.CreateProjectRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.ProjectDto;
import com.opencirc.api.passport.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrative endpoints for projects. */
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
@Tag(name = "Admin: Projects", description = "Administration of projects and their lifecycle")
public class ProjectController {

  private final ProjectService projectService;

  private final ActorContext actorContext;

  /** Creates a project together with its root passport. */
  @Operation(
      summary = "Creates a project; its root passport is provisioned in the same transaction")
  @PostMapping
  public ResponseEntity<ProjectDto> createProject(
      @Valid @RequestBody CreateProjectRequestDto request) {
    AdminDto admin = actorContext.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(projectService.createProject(request, CreatedByDto.from(admin), admin.getId()));
  }

  /** Lists projects, archived ones only on request. */
  @Operation(summary = "Lists projects; archived projects only with include_archived=true")
  @GetMapping
  public ResponseEntity<List<ProjectDto>> listProjects(
      @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived) {
    return ResponseEntity.ok(projectService.listProjects(includeArchived));
  }

  /** Retrieves a project. */
  @Operation(summary = "Retrieves a project; an archived one only with include_archived=true")
  @GetMapping("/{id}")
  public ResponseEntity<ProjectDto> getProject(
      @PathVariable String id,
      @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived) {
    return ResponseEntity.ok(projectService.getProject(id, includeArchived));
  }

  /** Soft-archives a project and every passport in it. */
  @Operation(summary = "Soft-archives a project and its passport tree; nothing is deleted")
  @DeleteMapping("/{id}")
  public ResponseEntity<ProjectDto> archiveProject(@PathVariable String id) {
    return ResponseEntity.ok(projectService.archiveProject(id));
  }

  /** Restores an archived project. */
  @Operation(summary = "Restores an archived project and its root passport")
  @PostMapping("/{id}/restore")
  public ResponseEntity<ProjectDto> restoreProject(@PathVariable String id) {
    return ResponseEntity.ok(projectService.restoreProject(id));
  }
}
