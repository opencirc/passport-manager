package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dao.AuditLogRepository;
import com.opencirc.api.passport.dao.AuditLogTagRepository;
import com.opencirc.api.passport.dto.AuditLogDto;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.ValidationStatus;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.AuditLogTag;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.AuditLogService;
import com.opencirc.api.passport.service.JsonDiffService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TestAuditLogService {

  private static final ApiKeyPrincipal ACTOR =
      new ApiKeyPrincipal("key-1", "Key", "group-1", "project-1", AccessGroupPermissions.full());

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private AuditLogTagRepository auditLogTagRepository;
  @Mock private ActorContext actorContext;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private AuditLogService service;

  @BeforeEach
  void setUp() {
    service =
        new AuditLogService(
            auditLogRepository,
            auditLogTagRepository,
            new JsonDiffService(objectMapper),
            actorContext,
            objectMapper);
  }

  @Test
  void recordsDiffAttributedToTheRequestsApiKey() {
    when(actorContext.currentApiKey()).thenReturn(Optional.of(ACTOR));
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));
    Passport before = passport("p1", "Wall");
    Passport after = passport("p1", "Wall A");

    service.record(
        AuditTargetType.PASSPORT,
        "p1",
        "project-1",
        "p1",
        AuditLogAction.UPDATE,
        service.passportSnapshot(before),
        service.passportSnapshot(after));

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    AuditLog log = captor.getValue();
    assertEquals("key-1", log.getCreatedById());
    assertEquals(AuditTargetType.PASSPORT, log.getTargetType());
    assertEquals("project-1", log.getProjectId());
    assertEquals("p1", log.getPassportId());
    assertEquals(1, log.getDiff().size());
    assertEquals("replace", log.getDiff().get(0).get("op").asText());
    assertEquals("/name", log.getDiff().get(0).get("path").asText());
    assertEquals("Wall A", log.getDiff().get(0).get("value").asText());
  }

  @Test
  void recordsSystemActionsWithoutCreator() {
    when(actorContext.currentApiKey()).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

    service.record(
        AuditTargetType.PASSPORT,
        "root",
        "project-1",
        "root",
        AuditLogAction.CREATE,
        null,
        service.passportSnapshot(passport("root", "Building")));

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertNull(captor.getValue().getCreatedById());
    assertEquals("add", captor.getValue().getDiff().get(0).get("op").asText());
    assertEquals("", captor.getValue().getDiff().get(0).get("path").asText());
    assertEquals("Building", captor.getValue().getDiff().get(0).get("value").get("name").asText());
  }

  @Test
  void projectFeedCarriesValidationTags() {
    AuditLog log = new AuditLog();
    log.setId("log-1");
    log.setTargetType(AuditTargetType.PASSPORT_DATASHEET);
    log.setTargetId("ds-1");
    log.setProjectId("project-1");
    log.setAction(AuditLogAction.UPDATE);
    log.setDiff(objectMapper.createArrayNode());
    AuditLogTag tag = new AuditLogTag();
    tag.setId("tag-1");
    tag.setLogId("log-1");
    tag.setTag(ValidationStatus.APPROVED);
    tag.setEmail("auditor@example.org");
    tag.setApiKeyId("key-1");
    tag.setSignature("abc");
    when(auditLogRepository.findByProjectIdOrderByCreatedTimeDesc("project-1"))
        .thenReturn(List.of(log));
    when(auditLogTagRepository.findByLogIdIn(List.of("log-1"))).thenReturn(List.of(tag));

    List<AuditLogDto> feed = service.findByProjectId("project-1");

    assertEquals(1, feed.size());
    assertNotNull(feed.get(0).getValidationTag());
    assertEquals("approved", feed.get(0).getValidationTag().getTag());
    assertEquals("UPDATE", feed.get(0).getAction());
    assertEquals("PASSPORT_DATASHEET", feed.get(0).getTargetType());
  }

  @Test
  void targetFeedHidesEntriesOfOtherProjects() {
    AuditLog own = new AuditLog();
    own.setId("own");
    own.setProjectId("project-1");
    own.setDiff(objectMapper.createArrayNode());
    AuditLog foreign = new AuditLog();
    foreign.setId("foreign");
    foreign.setProjectId("project-2");
    foreign.setDiff(objectMapper.createArrayNode());
    AuditLog global = new AuditLog();
    global.setId("global");
    global.setDiff(objectMapper.createArrayNode());
    when(auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedTimeAsc(
            AuditTargetType.PASSPORT, "p1"))
        .thenReturn(List.of(own, foreign, global));
    when(auditLogTagRepository.findByLogIdIn(any())).thenReturn(List.of());

    List<AuditLogDto> feed = service.findByTarget(AuditTargetType.PASSPORT, "p1", ACTOR);

    assertEquals(List.of("own", "global"), feed.stream().map(AuditLogDto::getId).toList());
  }

  private static Passport passport(String id, String name) {
    Passport passport = new Passport();
    passport.setId(id);
    passport.setProjectId("project-1");
    passport.setName(name);
    passport.setStatus(Passport.Status.ACTIVE);
    return passport;
  }
}
