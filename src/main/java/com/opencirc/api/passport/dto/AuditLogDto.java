package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.AuditLogTag;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

/** Audit log entry with its RFC 6902 diff and, when present, its validation tag. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {

  private String id;

  private String targetType;

  private String targetId;

  private String projectId;

  private String passportId;

  private String action;

  private JsonNode diff;

  private String createdById;

  private OffsetDateTime createdTime;

  private AuditLogTagDto validationTag;

  /** Maps an AuditLog and its optional tag to the DTO. */
  public static AuditLogDto from(AuditLog log, AuditLogTag tag) {
    return new AuditLogDto(
        log.getId(),
        log.getTargetType() != null ? log.getTargetType().getValue() : null,
        log.getTargetId(),
        log.getProjectId(),
        log.getPassportId(),
        log.getAction() != null ? log.getAction().getValue() : null,
        log.getDiff(),
        log.getCreatedById(),
        log.getCreatedTime(),
        tag != null ? AuditLogTagDto.from(tag) : null);
  }
}
