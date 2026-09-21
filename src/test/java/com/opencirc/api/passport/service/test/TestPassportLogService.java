package com.opencirc.api.passport.service.test;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.PassportLog;
import com.opencirc.api.passport.service.PassportLogService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestPassportLogService {

  @Mock private PassportLogRepository passportLogRepository;

  @Mock private UserContext userContext;

  private ObjectMapper objectMapper = new ObjectMapper();

  private PassportLogService passportLogService;

  @BeforeEach
  public void setUp() {
    passportLogService = new PassportLogService(passportLogRepository, userContext, objectMapper);
  }

  @Test
  public void shouldLogEvent() {
    String passportId = "test-passport-id";
    PassportLogAction action = PassportLogAction.CREATE;
    java.util.List<Map<String, Object>> changes =
        java.util.List.of(Map.of("info", "Passport created"));

    UserDto userDto = new UserDto();
    userDto.setId("test-user-id");
    userDto.setFullName("Test User");
    userDto.setEmail("test@example.com");

    when(userContext.getCurrentUser()).thenReturn(userDto);

    passportLogService.logEvent(passportId, action, changes);

    ArgumentCaptor<PassportLog> logCaptor = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(logCaptor.capture());

    PassportLog capturedLog = logCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(passportId, capturedLog.getPassportId());
    org.junit.jupiter.api.Assertions.assertEquals("test-user-id", capturedLog.getCreatedById());
    org.junit.jupiter.api.Assertions.assertNotNull(capturedLog.getCreatedBy());
    org.junit.jupiter.api.Assertions.assertEquals(
        "Test User", capturedLog.getCreatedBy().getFullName());
    org.junit.jupiter.api.Assertions.assertEquals(
        "CREATE", capturedLog.getData().get("action").asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        "Passport created", capturedLog.getData().get("changes").get(0).get("info").asText());
  }

  @Test
  public void shouldLogEventWithExplicitCreatedBy() {
    String passportId = "test-passport-id-2";
    PassportLogAction action = PassportLogAction.EPD_ENRICHMENT_FAILED;
    Map<String, String> changes = Map.of("reason", "Unsafe URL");
    com.opencirc.api.passport.dto.CreatedByDto createdBy =
        new com.opencirc.api.passport.dto.CreatedByDto("Async Worker", "worker@opencirc.org");

    passportLogService.logEvent(passportId, action, changes, createdBy, "worker-id");

    ArgumentCaptor<PassportLog> logCaptor = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(logCaptor.capture());

    PassportLog capturedLog = logCaptor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(passportId, capturedLog.getPassportId());
    org.junit.jupiter.api.Assertions.assertEquals("worker-id", capturedLog.getCreatedById());
    org.junit.jupiter.api.Assertions.assertEquals(
        "Async Worker", capturedLog.getCreatedBy().getFullName());
    org.junit.jupiter.api.Assertions.assertEquals(
        "EPD_ENRICHMENT_FAILED", capturedLog.getData().get("action").asText());
  }

  @Test
  public void shouldUseSystemOnlyWhenAuthenticationIsMissing() {
    when(userContext.getCurrentUser())
        .thenThrow(
            new org.springframework.security.authentication
                .AuthenticationCredentialsNotFoundException("Missing"));
    passportLogService.logEvent("passport", PassportLogAction.CREATE, java.util.List.of());
    ArgumentCaptor<PassportLog> captured = ArgumentCaptor.forClass(PassportLog.class);
    verify(passportLogRepository).save(captured.capture());
    org.junit.jupiter.api.Assertions.assertEquals("system", captured.getValue().getCreatedById());
  }

  @Test
  public void shouldPropagateUnexpectedActorFailures() {
    IllegalStateException failure = new IllegalStateException("Broken context");
    when(userContext.getCurrentUser()).thenThrow(failure);
    org.junit.jupiter.api.Assertions.assertSame(
        failure,
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () ->
                passportLogService.logEvent(
                    "passport", PassportLogAction.CREATE, java.util.List.of())));
    org.mockito.Mockito.verifyNoInteractions(passportLogRepository);
  }

  @Test
  public void shouldGetLogsByPassportId() {
    String passportId = "test-passport-id";
    PassportLog log = new PassportLog();
    log.setPassportId(passportId);
    log.setId("log-id");
    log.setCreatedById("actor-id");
    log.setCreatedBy(
        new com.opencirc.api.passport.dto.CreatedByDto("Editor", "editor@example.com"));
    log.setCreatedTime(java.time.OffsetDateTime.parse("2026-09-21T12:00:00Z"));
    log.setData(objectMapper.createObjectNode().put("action", "CREATE"));
    when(passportLogRepository.findByPassportIdOrderByCreatedTimeAsc(passportId))
        .thenReturn(java.util.List.of(log));

    java.util.List<com.opencirc.api.passport.dto.PassportLogDto> logs =
        passportLogService.getLogsByPassportId(passportId);

    org.junit.jupiter.api.Assertions.assertEquals(1, logs.size());
    org.junit.jupiter.api.Assertions.assertEquals(passportId, logs.get(0).getPassportId());
    org.junit.jupiter.api.Assertions.assertEquals(log.getId(), logs.get(0).getId());
    org.junit.jupiter.api.Assertions.assertEquals(
        log.getCreatedById(), logs.get(0).getCreatedById());
    org.junit.jupiter.api.Assertions.assertEquals(log.getCreatedBy(), logs.get(0).getCreatedBy());
    org.junit.jupiter.api.Assertions.assertEquals(log.getData(), logs.get(0).getData());
    org.junit.jupiter.api.Assertions.assertEquals(
        log.getCreatedTime(), logs.get(0).getCreatedTime());
    verify(passportLogRepository).findByPassportIdOrderByCreatedTimeAsc(passportId);
  }
}
