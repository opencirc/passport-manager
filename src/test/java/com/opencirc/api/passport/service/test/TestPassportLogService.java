package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.PassportLogDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.Admin;
import com.opencirc.api.passport.model.PassportLog;
import com.opencirc.api.passport.service.PassportLogService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TestPassportLogService {

  @Mock private PassportLogRepository passportLogRepository;

  @Mock private ActorContext actorContext;

  private ObjectMapper objectMapper = new ObjectMapper();

  private PassportLogService passportLogService;

  @BeforeEach
  public void setUp() {
    passportLogService = new PassportLogService(passportLogRepository, actorContext, objectMapper);
  }

  @Test
  public void shouldAttributeEventToApiKey() {
    String passportId = "test-passport-id";
    List<Map<String, Object>> changes = List.of(Map.of("info", "Passport created"));
    when(actorContext.currentApiKey())
        .thenReturn(
            Optional.of(
                new ApiKeyPrincipal(
                    "key-1",
                    "Revit pipeline",
                    "group-1",
                    "project-1",
                    AccessGroupPermissions.full())));

    passportLogService.logEvent(passportId, PassportLogAction.CREATE, changes);

    ArgumentCaptor<PassportLog> logCaptor = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(logCaptor.capture());

    PassportLog capturedLog = logCaptor.getValue();
    assertEquals(passportId, capturedLog.getPassportId());
    assertEquals("key-1", capturedLog.getCreatedById());
    assertNotNull(capturedLog.getCreatedBy());
    assertEquals("Revit pipeline", capturedLog.getCreatedBy().getFullName());
    assertEquals("CREATE", capturedLog.getData().get("action").asText());
    assertEquals(
        "Passport created", capturedLog.getData().get("changes").get(0).get("info").asText());
  }

  @Test
  public void shouldAttributeEventToAdmin() {
    Admin admin = new Admin();
    admin.setId("admin-id");
    admin.setFirstName("Test");
    admin.setLastName("Admin");
    admin.setEmail("admin@example.com");
    admin.setPassword("secret");
    admin.setActive(true);
    when(actorContext.currentApiKey()).thenReturn(Optional.empty());
    when(actorContext.currentAdmin()).thenReturn(Optional.of(new AdminPrincipal(admin)));

    passportLogService.logEvent("passport", PassportLogAction.CREATE, List.of());

    ArgumentCaptor<PassportLog> captured = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(captured.capture());
    assertEquals("admin-id", captured.getValue().getCreatedById());
    assertEquals("Test Admin", captured.getValue().getCreatedBy().getFullName());
    assertEquals("admin@example.com", captured.getValue().getCreatedBy().getEmail());
  }

  @Test
  public void shouldLogEventWithExplicitCreatedBy() {
    String passportId = "test-passport-id-2";
    PassportLogAction action = PassportLogAction.EPD_ENRICHMENT_FAILED;
    Map<String, String> changes = Map.of("reason", "Unsafe URL");
    CreatedByDto createdBy = new CreatedByDto("Async Worker", "worker@opencirc.org");

    passportLogService.logEvent(passportId, action, changes, createdBy, "worker-id");

    ArgumentCaptor<PassportLog> logCaptor = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(logCaptor.capture());

    PassportLog capturedLog = logCaptor.getValue();
    assertEquals(passportId, capturedLog.getPassportId());
    assertEquals("worker-id", capturedLog.getCreatedById());
    assertEquals("Async Worker", capturedLog.getCreatedBy().getFullName());
    assertEquals("EPD_ENRICHMENT_FAILED", capturedLog.getData().get("action").asText());
  }

  @Test
  public void shouldUseSystemOnlyWhenAuthenticationIsMissing() {
    when(actorContext.currentApiKey()).thenReturn(Optional.empty());
    when(actorContext.currentAdmin()).thenReturn(Optional.empty());

    passportLogService.logEvent("passport", PassportLogAction.CREATE, List.of());

    ArgumentCaptor<PassportLog> captured = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(captured.capture());
    assertEquals("system", captured.getValue().getCreatedById());
  }

  @Test
  public void shouldPropagateUnexpectedActorFailures() {
    IllegalStateException failure = new IllegalStateException("Broken context");
    when(actorContext.currentApiKey()).thenThrow(failure);
    org.junit.jupiter.api.Assertions.assertSame(
        failure,
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> passportLogService.logEvent("passport", PassportLogAction.CREATE, List.of())));
    org.mockito.Mockito.verifyNoInteractions(passportLogRepository);
  }

  @Test
  public void shouldGetLogsByPassportId() {
    String passportId = "test-passport-id";
    PassportLog log = new PassportLog();
    log.setPassportId(passportId);
    log.setId("log-id");
    log.setCreatedById("actor-id");
    log.setCreatedBy(new CreatedByDto("Editor", "editor@example.com"));
    log.setCreatedTime(java.time.OffsetDateTime.parse("2026-09-21T12:00:00Z"));
    log.setData(objectMapper.createObjectNode().put("action", "CREATE"));
    when(passportLogRepository.findByPassportIdOrderByCreatedTimeAsc(passportId))
        .thenReturn(List.of(log));

    List<PassportLogDto> logs = passportLogService.getLogsByPassportId(passportId);

    assertEquals(1, logs.size());
    assertEquals(passportId, logs.get(0).getPassportId());
    assertEquals(log.getId(), logs.get(0).getId());
    assertEquals(log.getCreatedById(), logs.get(0).getCreatedById());
    assertEquals(log.getCreatedBy(), logs.get(0).getCreatedBy());
    assertEquals(log.getData(), logs.get(0).getData());
    assertEquals(log.getCreatedTime(), logs.get(0).getCreatedTime());
    verify(passportLogRepository).findByPassportIdOrderByCreatedTimeAsc(passportId);
  }
}
