package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dto.AuditLogDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.service.AuditLogService;
import com.opencirc.api.passport.service.PassportService;
import com.opencirc.api.passport.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only administrative view of what the machine integrations have done in a project: its
 * passports and the audit trail. Administrators never author passport data (ADR 0001); they may
 * inspect it without minting a machine token (ADR 0010).
 */
@RestController
@RequestMapping("/api/admin/projects/{projectId}")
@RequiredArgsConstructor
@Tag(
    name = "Admin: Project inspection",
    description = "Read-only passports and audit trail of a project for administrators")
public class ProjectInspectionController {

  private final ProjectService projectService;

  private final PassportService passportService;

  private final AuditLogService auditLogService;

  private final ActorContext actorContext;

  /** The root passport of the project, whatever its status. */
  @Operation(summary = "Retrieves the root passport of the project")
  @GetMapping("/root-passport")
  public ResponseEntity<PassportDto> getRootPassport(@PathVariable String projectId) {
    requireProject(projectId);
    return ResponseEntity.ok(passportService.getRootPassportInProject(projectId));
  }

  /** A passport of the project with its datasheets. */
  @Operation(
      summary =
          "Retrieves a passport of the project; an archived one only with include_archived=true")
  @GetMapping("/passports/{passportId}")
  public ResponseEntity<PassportDto> getPassport(
      @PathVariable String projectId,
      @PathVariable String passportId,
      @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived) {
    requireProject(projectId);
    return ResponseEntity.ok(
        passportService.getPassportInProject(passportId, projectId, includeArchived));
  }

  /** The non-archived passports directly below a passport of the project. */
  @Operation(summary = "Lists the immediate children of a passport of the project")
  @GetMapping("/passports/{passportId}/children")
  public ResponseEntity<List<PassportDto>> getChildren(
      @PathVariable String projectId, @PathVariable String passportId) {
    requireProject(projectId);
    return ResponseEntity.ok(passportService.getImmediateChildrenInProject(passportId, projectId));
  }

  /** Newest-first activity stream of the project. */
  @Operation(summary = "Project-wide audit trail, newest first")
  @GetMapping("/audit-logs")
  public ResponseEntity<List<AuditLogDto>> getAuditLogs(@PathVariable String projectId) {
    requireProject(projectId);
    return ResponseEntity.ok(auditLogService.findByProjectId(projectId));
  }

  /** Newest-first activity stream of one passport of the project. */
  @Operation(summary = "Audit trail of a passport of the project, newest first")
  @GetMapping("/passports/{passportId}/audit-logs")
  public ResponseEntity<List<AuditLogDto>> getPassportAuditLogs(
      @PathVariable String projectId, @PathVariable String passportId) {
    requireProject(projectId);
    passportService.getPassportInProject(passportId, projectId, true);
    return ResponseEntity.ok(auditLogService.findByPassportId(passportId));
  }

  private void requireProject(String projectId) {
    actorContext.requireAdmin();
    projectService.getProject(projectId, true);
  }
}
