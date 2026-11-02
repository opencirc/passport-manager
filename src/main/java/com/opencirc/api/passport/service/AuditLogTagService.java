package com.opencirc.api.passport.service;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dao.AuditLogTagRepository;
import com.opencirc.api.passport.dto.AuditLogTagDto;
import com.opencirc.api.passport.dto.SignatureVerificationDto;
import com.opencirc.api.passport.enums.ValidationStatus;
import com.opencirc.api.passport.exception.ForbiddenException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.AuditLogTag;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Attaches signed validation tags to audit log entries and re-verifies them. Signing uses the raw
 * secret of the API key presented with the request; since only its hash is stored, verification
 * must be requested with the same key's token.
 */
@Service
public class AuditLogTagService {

  private final AuditLogTagRepository auditLogTagRepository;

  private final AuditLogService auditLogService;

  private final SignatureService signatureService;

  /** Constructor. */
  public AuditLogTagService(
      AuditLogTagRepository auditLogTagRepository,
      AuditLogService auditLogService,
      SignatureService signatureService) {
    this.auditLogTagRepository = auditLogTagRepository;
    this.auditLogService = auditLogService;
    this.signatureService = signatureService;
  }

  /** Creates the tag of a log entry or transitions its state, signing it with the actor's key. */
  @Transactional
  public AuditLogTagDto tag(
      String logId, ValidationStatus status, String email, ApiKeyPrincipal actor) {
    if (status == null) {
      throw new InvalidInputException("A validation tag is required");
    }
    if (email == null || email.isBlank()) {
      throw new InvalidInputException("The validator email is required");
    }
    AuditLog log = auditLogService.requireLog(logId);
    assertVisible(log, actor);
    String secret = requireSecret(actor);

    AuditLogTag tag = auditLogTagRepository.findByLogId(logId).orElseGet(AuditLogTag::new);
    boolean transition = tag.getId() != null;
    tag.setLogId(log.getId());
    tag.setTag(status);
    tag.setEmail(email.trim());
    tag.setApiKeyId(actor.getKeyId());
    tag.setSignature(
        signatureService.sign(
            log.getId(),
            log.getTargetId(),
            tag.getEmail(),
            status.getValue(),
            log.getDiff(),
            secret));
    tag.setUpdatedTime(transition ? OffsetDateTime.now() : null);
    return AuditLogTagDto.from(auditLogTagRepository.save(tag));
  }

  /** Re-computes the signature of a log entry's tag with the actor's key and compares. */
  @Transactional(readOnly = true)
  public SignatureVerificationDto verify(String logId, ApiKeyPrincipal actor) {
    AuditLog log = auditLogService.requireLog(logId);
    assertVisible(log, actor);
    AuditLogTag tag =
        auditLogTagRepository
            .findByLogId(logId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Audit log entry has no validation tag: " + logId));

    SignatureVerificationDto result = new SignatureVerificationDto();
    result.setLogId(log.getId());
    result.setApiKeyId(tag.getApiKeyId());
    result.setTag(tag.getTag() != null ? tag.getTag().getValue() : null);
    result.setEmail(tag.getEmail());

    if (!actor.getKeyId().equals(tag.getApiKeyId())) {
      result.setValid(false);
      result.setReason("The tag was signed by another API key");
      return result;
    }
    boolean valid =
        signatureService.verify(
            tag.getSignature(),
            log.getId(),
            log.getTargetId(),
            tag.getEmail(),
            result.getTag(),
            log.getDiff(),
            requireSecret(actor));
    result.setValid(valid);
    result.setReason(valid ? null : "The signature does not match the current state");
    return result;
  }

  private static void assertVisible(AuditLog log, ApiKeyPrincipal actor) {
    if (log.getProjectId() != null && !log.getProjectId().equals(actor.getProjectId())) {
      throw new ForbiddenException("Audit log entry belongs to another project");
    }
  }

  private static String requireSecret(ApiKeyPrincipal actor) {
    if (actor.getRawSecret() == null || actor.getRawSecret().isEmpty()) {
      throw new AuthenticationCredentialsNotFoundException(
          "Signing requires the request to carry the API key token");
    }
    return actor.getRawSecret();
  }
}
