package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.AuditLogTag;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Signed validation tag of an audit log entry. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogTagDto {

  private String id;

  private String logId;

  private String tag;

  private String email;

  private String apiKeyId;

  private String signature;

  private OffsetDateTime createdTime;

  private OffsetDateTime updatedTime;

  /** Maps an AuditLogTag to its DTO. */
  public static AuditLogTagDto from(AuditLogTag tag) {
    return new AuditLogTagDto(
        tag.getId(),
        tag.getLogId(),
        tag.getTag() != null ? tag.getTag().getValue() : null,
        tag.getEmail(),
        tag.getApiKeyId(),
        tag.getSignature(),
        tag.getCreatedTime(),
        tag.getUpdatedTime());
  }
}
