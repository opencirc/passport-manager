package com.opencirc.api.passport.dao.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportLog;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {PassportManager.class, TestConfig.class})
@ActiveProfiles("test")
public class TestPassportLogRepository {

  @Autowired private PassportLogRepository passportLogRepository;

  @Autowired private PassportRepository passportRepository;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    passportLogRepository.deleteAll();
    passportRepository.deleteAll();
  }

  @Test
  public void shouldSaveAndRetrievePassportLog() {
    String passportId = "test-passport-id";
    CreatedByDto createdBy = new CreatedByDto("Test User", "test@example.com");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setName("Test Passport");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setCreatedBy(createdBy);
    passport.setCreatedById("test-user-id");
    passportRepository.save(passport);

    ObjectNode data = objectMapper.createObjectNode();
    data.put("action", "CREATE");

    PassportLog log = new PassportLog();
    log.setPassportId(passportId);
    log.setCreatedBy(createdBy);
    log.setCreatedById("test-user-id");
    log.setData(data);

    passportLogRepository.save(log);

    List<PassportLog> logs = passportLogRepository.findByPassportId(passportId);
    assertNotNull(logs);
    assertEquals(1, logs.size());
    assertEquals(passportId, logs.get(0).getPassportId());
    assertEquals("CREATE", logs.get(0).getData().get("action").asText());
  }
}
