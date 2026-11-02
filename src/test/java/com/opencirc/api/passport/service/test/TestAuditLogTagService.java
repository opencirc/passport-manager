package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dao.AuditLogTagRepository;
import com.opencirc.api.passport.dto.AuditLogTagDto;
import com.opencirc.api.passport.dto.SignatureVerificationDto;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.ValidationStatus;
import com.opencirc.api.passport.exception.ForbiddenException;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.AuditLogTag;
import com.opencirc.api.passport.service.AuditLogService;
import com.opencirc.api.passport.service.AuditLogTagService;
import com.opencirc.api.passport.service.JsonDiffService;
import com.opencirc.api.passport.service.SignatureService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TestAuditLogTagService {

  private static final ApiKeyPrincipal SIGNER =
      new ApiKeyPrincipal(
          "key-1", "Key", "group-1", "project-1", AccessGroupPermissions.full(), "s3cret");

  @Mock private AuditLogTagRepository auditLogTagRepository;
  @Mock private AuditLogService auditLogService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private AuditLogTagService service;
  private AuditLog log;

  @BeforeEach
  void setUp() {
    service =
        new AuditLogTagService(
            auditLogTagRepository,
            auditLogService,
            new SignatureService(new JsonDiffService(objectMapper)));
    log = new AuditLog();
    log.setId("log-1");
    log.setTargetType(AuditTargetType.PASSPORT_DATASHEET);
    log.setTargetId("ds-1");
    log.setProjectId("project-1");
    log.setAction(AuditLogAction.UPDATE);
    log.setDiff(
        objectMapper.readTree(
            "[{\"op\":\"replace\",\"path\":\"/data/fireRating\",\"value\":\"EI60\"}]"));
    when(auditLogService.requireLog("log-1")).thenReturn(log);
  }

  @Test
  void signsNewTagWithTheRequestsKeySecret() {
    when(auditLogTagRepository.findByLogId("log-1")).thenReturn(Optional.empty());
    when(auditLogTagRepository.save(any(AuditLogTag.class))).thenAnswer(i -> i.getArgument(0));

    AuditLogTagDto tag =
        service.tag("log-1", ValidationStatus.APPROVED, "lead.auditor@certifier.org", SIGNER);

    assertEquals("approved", tag.getTag());
    assertEquals("key-1", tag.getApiKeyId());
    assertNotNull(tag.getSignature());
    assertEquals(64, tag.getSignature().length());
  }

  @Test
  void transitionsExistingTagAndKeepsItUnique() {
    AuditLogTag existing = new AuditLogTag();
    existing.setId("tag-1");
    existing.setLogId("log-1");
    existing.setTag(ValidationStatus.PENDING);
    existing.setEmail("lead.auditor@certifier.org");
    existing.setApiKeyId("key-1");
    existing.setSignature("old");
    when(auditLogTagRepository.findByLogId("log-1")).thenReturn(Optional.of(existing));
    when(auditLogTagRepository.save(any(AuditLogTag.class))).thenAnswer(i -> i.getArgument(0));

    AuditLogTagDto tag =
        service.tag("log-1", ValidationStatus.REJECTED, "lead.auditor@certifier.org", SIGNER);

    assertEquals("tag-1", tag.getId());
    assertEquals("rejected", tag.getTag());
    assertNotNull(tag.getUpdatedTime());
  }

  @Test
  void verifiesAnUntouchedTagAndDetectsTampering() {
    when(auditLogTagRepository.findByLogId("log-1")).thenReturn(Optional.empty());
    when(auditLogTagRepository.save(any(AuditLogTag.class))).thenAnswer(i -> i.getArgument(0));
    AuditLogTagDto signed =
        service.tag("log-1", ValidationStatus.APPROVED, "lead.auditor@certifier.org", SIGNER);

    AuditLogTag stored = new AuditLogTag();
    stored.setLogId("log-1");
    stored.setTag(ValidationStatus.APPROVED);
    stored.setEmail("lead.auditor@certifier.org");
    stored.setApiKeyId("key-1");
    stored.setSignature(signed.getSignature());
    when(auditLogTagRepository.findByLogId("log-1")).thenReturn(Optional.of(stored));

    SignatureVerificationDto verification = service.verify("log-1", SIGNER);
    assertTrue(verification.isValid());

    log.setDiff(
        objectMapper.readTree(
            "[{\"op\":\"replace\",\"path\":\"/data/fireRating\",\"value\":\"EI30\"}]"));
    SignatureVerificationDto tampered = service.verify("log-1", SIGNER);
    assertFalse(tampered.isValid());
    assertNotNull(tampered.getReason());
  }

  @Test
  void reportsTagsSignedByAnotherKeyAsNotVerifiable() {
    AuditLogTag stored = new AuditLogTag();
    stored.setLogId("log-1");
    stored.setTag(ValidationStatus.APPROVED);
    stored.setEmail("lead.auditor@certifier.org");
    stored.setApiKeyId("key-2");
    stored.setSignature("whatever");
    when(auditLogTagRepository.findByLogId("log-1")).thenReturn(Optional.of(stored));

    SignatureVerificationDto verification = service.verify("log-1", SIGNER);

    assertFalse(verification.isValid());
    assertEquals("key-2", verification.getApiKeyId());
  }

  @Test
  void refusesToSignWithoutTheTokenSecretOrOutsideTheProject() {
    ApiKeyPrincipal withoutSecret =
        new ApiKeyPrincipal("key-1", "Key", "group-1", "project-1", AccessGroupPermissions.full());
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> service.tag("log-1", ValidationStatus.APPROVED, "a@b.org", withoutSecret));

    ApiKeyPrincipal otherProject =
        new ApiKeyPrincipal(
            "key-9", "Key", "group-9", "project-2", AccessGroupPermissions.full(), "x");
    assertThrows(
        ForbiddenException.class,
        () -> service.tag("log-1", ValidationStatus.APPROVED, "a@b.org", otherProject));
  }
}
