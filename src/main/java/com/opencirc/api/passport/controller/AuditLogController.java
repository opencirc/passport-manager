package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.AuditLogDto;
import com.opencirc.api.passport.dto.AuditLogTagDto;
import com.opencirc.api.passport.dto.AuditLogTagRequestDto;
import com.opencirc.api.passport.dto.SignatureVerificationDto;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.ValidationStatus;
import com.opencirc.api.passport.exception.ForbiddenException;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.AuditLogService;
import com.opencirc.api.passport.service.AuditLogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Machine-facing audit feeds and signed validation tags, scoped to the key's project. */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "RFC 6902 audit trails and signed validation tags")
public class AuditLogController {

  private final AuditLogService auditLogService;

  private final AuditLogTagService auditLogTagService;

  private final PassportRepository passportRepository;

  private final ActorContext actorContext;

  /** Chronological history of one entity. */
  @Operation(summary = "Chronological audit history of a passport, datasheet or definition")
  @GetMapping("/target/{targetType}/{targetId}")
  public ResponseEntity<List<AuditLogDto>> getByTarget(
      @PathVariable String targetType, @PathVariable String targetId) {
    return ResponseEntity.ok(
        auditLogService.findByTarget(
            AuditTargetType.fromValue(targetType), targetId, actorContext.requireApiKey()));
  }

  /** Newest-first activity stream of the key's project. */
  @Operation(summary = "Project-wide activity stream, newest first")
  @GetMapping("/project/{projectId}")
  public ResponseEntity<List<AuditLogDto>> getByProject(@PathVariable String projectId) {
    ApiKeyPrincipal actor = actorContext.requireApiKey();
    if (!projectId.equals(actor.getProjectId())) {
      throw new ForbiddenException("The API key does not belong to this project");
    }
    return ResponseEntity.ok(auditLogService.findByProjectId(projectId));
  }

  /** Newest-first activity stream of a passport in the key's project. */
  @Operation(summary = "Passport-wide activity stream, newest first")
  @GetMapping("/passport/{passportId}")
  public ResponseEntity<List<AuditLogDto>> getByPassport(@PathVariable String passportId) {
    ApiKeyPrincipal actor = actorContext.requireApiKey();
    Passport passport =
        passportRepository
            .findById(passportId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));
    if (!actor.getProjectId().equals(passport.getProjectId())) {
      throw new ForbiddenException("Passport belongs to another project");
    }
    return ResponseEntity.ok(auditLogService.findByPassportId(passportId));
  }

  /** Attaches or transitions the signed validation tag of a log entry. */
  @Operation(summary = "Creates or transitions the validation tag of an audit log entry")
  @PostMapping("/{logId}/tags")
  public ResponseEntity<AuditLogTagDto> tag(
      @PathVariable String logId, @Valid @RequestBody AuditLogTagRequestDto request) {
    return ResponseEntity.ok(
        auditLogTagService.tag(
            logId,
            ValidationStatus.fromValue(request.getTag()),
            request.getEmail(),
            actorContext.requireApiKey()));
  }

  /** Re-verifies the tag's signature with the calling key. */
  @Operation(summary = "Verifies the signature of an audit log entry's tag with the calling key")
  @GetMapping("/{logId}/tag/verify")
  public ResponseEntity<SignatureVerificationDto> verify(@PathVariable String logId) {
    return ResponseEntity.ok(auditLogTagService.verify(logId, actorContext.requireApiKey()));
  }
}
