package com.opencirc.api.passport.dao.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.dao.AuditLogRepository;
import com.opencirc.api.passport.dao.AuditLogTagRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.Project;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {PassportManager.class, TestConfig.class})
@ActiveProfiles("test")
public class TestAuditLogRepository {

  @Autowired private AuditLogRepository auditLogRepository;

  @Autowired private AuditLogTagRepository auditLogTagRepository;

  @Autowired private PassportRepository passportRepository;

  @Autowired private ProjectRepository projectRepository;

  @Autowired private ObjectMapper objectMapper;

  @Autowired public org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  private String projectId;

  @BeforeEach
  public void setUp() {
    // Tags restrict api_keys deletion, so they go first; projects then cascade the rest.
    auditLogTagRepository.deleteAllInBatch();
    auditLogRepository.deleteAllInBatch();
    projectRepository.deleteAllInBatch();

    Project project = new Project();
    project.setName("Audit repository project");
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedBy(new CreatedByDto("Test User", "test@example.com"));
    projectId = projectRepository.save(project).getId();
  }

  @Test
  public void entityHistoryIsChronologicalAndFeedsAreNewestFirst() {
    savePassport("requested", null);
    savePassport("other", "requested");
    AuditLog later = saveLog("requested", AuditLogAction.UPDATE);
    AuditLog earlier = saveLog("requested", AuditLogAction.CREATE);
    AuditLog otherLog = saveLog("other", AuditLogAction.CREATE);
    jdbcTemplate.update(
        "UPDATE audit_logs SET created_time = ?::timestamptz WHERE id = ?",
        "2026-09-21T12:00:00Z",
        later.getId());
    jdbcTemplate.update(
        "UPDATE audit_logs SET created_time = ?::timestamptz WHERE id = ?",
        "2026-09-21T11:00:00Z",
        earlier.getId());
    jdbcTemplate.update(
        "UPDATE audit_logs SET created_time = ?::timestamptz WHERE id = ?",
        "2026-09-21T10:00:00Z",
        otherLog.getId());

    List<AuditLog> history =
        auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedTimeAsc(
            AuditTargetType.PASSPORT, "requested");
    assertEquals(
        List.of(earlier.getId(), later.getId()), history.stream().map(AuditLog::getId).toList());

    List<AuditLog> passportFeed =
        auditLogRepository.findByPassportIdOrderByCreatedTimeDesc("requested");
    assertEquals(
        List.of(later.getId(), earlier.getId()),
        passportFeed.stream().map(AuditLog::getId).toList());

    List<AuditLog> projectFeed =
        auditLogRepository.findByProjectIdOrderByCreatedTimeDesc(projectId);
    assertEquals(
        List.of(later.getId(), earlier.getId(), otherLog.getId()),
        projectFeed.stream().map(AuditLog::getId).toList());
  }

  @Test
  public void storesTheJsonPatchAsJsonb() {
    savePassport("p1", null);
    AuditLog saved = saveLog("p1", AuditLogAction.UPDATE);

    AuditLog reloaded = auditLogRepository.findById(saved.getId()).orElseThrow();
    assertEquals("replace", reloaded.getDiff().get(0).get("op").asText());
    assertEquals("/name", reloaded.getDiff().get(0).get("path").asText());
    assertEquals(AuditTargetType.PASSPORT, reloaded.getTargetType());
    assertEquals(AuditLogAction.UPDATE, reloaded.getAction());
  }

  private AuditLog saveLog(String passportId, AuditLogAction action) {
    AuditLog log = new AuditLog();
    log.setTargetType(AuditTargetType.PASSPORT);
    log.setTargetId(passportId);
    log.setProjectId(projectId);
    log.setPassportId(passportId);
    log.setAction(action);
    log.setDiff(
        objectMapper.readTree("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Wall\"}]"));
    return auditLogRepository.save(log);
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
}
