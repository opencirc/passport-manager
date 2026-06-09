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
}
