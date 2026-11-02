package com.opencirc.api.passport.dao.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportLog;
import com.opencirc.api.passport.model.Project;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(classes = {PassportManager.class, TestConfig.class})
@ActiveProfiles("test")
public class TestPassportLogRepository {

  @Autowired private PassportLogRepository passportLogRepository;

  @Autowired private PassportRepository passportRepository;

  @Autowired private ProjectRepository projectRepository;

  @Autowired private ObjectMapper objectMapper;

  @Autowired public org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  private String projectId;

  @BeforeEach
  public void setUp() {
    // Bulk deletes let the database cascades clear passport trees and their datasheets.
    passportLogRepository.deleteAllInBatch();
    projectRepository.deleteAllInBatch();

    Project project = new Project();
    project.setName("Log repository project");
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedBy(new CreatedByDto("Test User", "test@example.com"));
    projectId = projectRepository.save(project).getId();
  }

  @Test
  public void shouldFilterPassportAndOrderLogsByCreationTime() {
    CreatedByDto creator = new CreatedByDto("Test User", "test@example.com");
    savePassport("requested", null);
    savePassport("other", "requested");
    PassportLog later = saveLog("requested", creator);
    PassportLog earlier = saveLog("requested", creator);
    saveLog("other", creator);
    jdbcTemplate.update(
        "UPDATE passport_logs SET created_time = ?::timestamptz WHERE id = ?",
        "2026-09-21T12:00:00Z",
        later.getId());
    jdbcTemplate.update(
        "UPDATE passport_logs SET created_time = ?::timestamptz WHERE id = ?",
        "2026-09-21T11:00:00Z",
        earlier.getId());

    List<PassportLog> logs =
        passportLogRepository.findByPassportIdOrderByCreatedTimeAsc("requested");

    assertEquals(
        List.of(earlier.getId(), later.getId()), logs.stream().map(PassportLog::getId).toList());
  }

  public PassportLog saveLog(String passportId, CreatedByDto creator) {
    PassportLog log = new PassportLog();
    log.setPassportId(passportId);
    log.setCreatedBy(creator);
    log.setData(objectMapper.createObjectNode().put("action", "CREATE"));
    return passportLogRepository.save(log);
  }

  private Passport savePassport(String id, String parentId) {
    Passport passport = new Passport();
    passport.setId(id);
    passport.setProjectId(projectId);
    passport.setParentId(parentId);
    passport.setName("Passport " + id);
    passport.setStatus(Passport.Status.ACTIVE);
    return passportRepository.save(passport);
  }

  @Test
  public void shouldSaveAndRetrievePassportLog() {
    String passportId = "test-passport-id";
    CreatedByDto createdBy = new CreatedByDto("Test User", "test@example.com");

    savePassport(passportId, null);

    ObjectNode data = objectMapper.createObjectNode();
    data.put("action", "CREATE");

    PassportLog log = new PassportLog();
    log.setPassportId(passportId);
    log.setCreatedBy(createdBy);
    log.setCreatedById("test-user-id");
    log.setData(data);

    passportLogRepository.save(log);

    List<PassportLog> logs =
        passportLogRepository.findByPassportIdOrderByCreatedTimeAsc(passportId);
    assertNotNull(logs);
    assertEquals(1, logs.size());
    assertEquals(passportId, logs.get(0).getPassportId());
    assertEquals("CREATE", logs.get(0).getData().get("action").asText());
  }
}
