package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.dao.AuditLogRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.helper.test.MachineAuthTestHelper;
import com.opencirc.api.passport.helper.test.MachineAuthTestHelper.MachineFixture;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.AccessGroupService;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.ProjectService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

/** End-to-end: audit feeds, signing a tag with the key's token, verifying, and tamper detection. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@ActiveProfiles("test")
public class TestAuditLogEndpoints {

  @LocalServerPort private int port;

  @Autowired private ProjectService projectService;
  @Autowired private AccessGroupService accessGroupService;
  @Autowired private ApiKeyService apiKeyService;
  @Autowired private PassportRepository passportRepository;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MachineFixture fixture;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    fixture =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.full());
  }

  @Test
  void projectFeedContainsTheRootProvisioningEntry() {
    given()
        .header("Authorization", fixture.bearer())
        .when()
        .get("/api/audit-logs/project/{projectId}", fixture.projectId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("[0].targetType", equalTo("PASSPORT"))
        .body("[0].targetId", equalTo(fixture.rootPassportId()))
        .body("[0].action", equalTo("CREATE"))
        .body("[0].diff[0].op", equalTo("add"));
  }

  @Test
  void projectFeedOfAnotherProjectIsForbidden() {
    MachineFixture other =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.full());

    given()
        .header("Authorization", other.bearer())
        .when()
        .get("/api/audit-logs/project/{projectId}", fixture.projectId())
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void signsVerifiesAndDetectsTampering() {
    AuditLog log = recordedUpdate();

    String signature =
        given()
            .header("Authorization", fixture.bearer())
            .contentType(ContentType.JSON)
            .body("{\"tag\":\"APPROVED\",\"email\":\"lead.auditor@certifier.org\"}")
            .when()
            .post("/api/audit-logs/{logId}/tags", log.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("tag", equalTo("approved"))
            .body("apiKeyId", equalTo(fixture.apiKeyId()))
            .body("signature", notNullValue())
            .extract()
            .path("signature");

    given()
        .header("Authorization", fixture.bearer())
        .when()
        .get("/api/audit-logs/{logId}/tag/verify", log.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("valid", equalTo(true))
        .body("apiKeyId", equalTo(fixture.apiKeyId()));

    // A second validator transitions the state; the signature changes with it.
    given()
        .header("Authorization", fixture.bearer())
        .contentType(ContentType.JSON)
        .body("{\"tag\":\"REJECTED\",\"email\":\"lead.auditor@certifier.org\"}")
        .when()
        .post("/api/audit-logs/{logId}/tags", log.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("tag", equalTo("rejected"))
        .body("signature", org.hamcrest.Matchers.not(equalTo(signature)));

    // Tamper with the stored diff directly in the database: verification must now fail.
    jdbcTemplate.update(
        "UPDATE audit_logs SET diff = ?::jsonb WHERE id = ?",
        "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Tampered\"}]",
        log.getId());

    given()
        .header("Authorization", fixture.bearer())
        .when()
        .get("/api/audit-logs/{logId}/tag/verify", log.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("valid", equalTo(false))
        .body("reason", notNullValue());
  }

  @Test
  void anotherKeyCannotVerifyTheSignature() {
    AuditLog log = recordedUpdate();
    given()
        .header("Authorization", fixture.bearer())
        .contentType(ContentType.JSON)
        .body("{\"tag\":\"APPROVED\",\"email\":\"lead.auditor@certifier.org\"}")
        .when()
        .post("/api/audit-logs/{logId}/tags", log.getId())
        .then()
        .statusCode(HttpStatus.OK.value());

    var second = apiKeyService.createApiKey(fixture.accessGroupId(), "Second key", null);

    given()
        .header("Authorization", "Bearer " + second.getToken())
        .when()
        .get("/api/audit-logs/{logId}/tag/verify", log.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("valid", equalTo(false));
  }

  @Test
  void taggingNeedsCreatePermission() {
    AuditLog log = recordedUpdate();
    MachineFixture readOnly =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.readOnly());

    given()
        .header("Authorization", readOnly.bearer())
        .contentType(ContentType.JSON)
        .body("{\"tag\":\"APPROVED\",\"email\":\"lead.auditor@certifier.org\"}")
        .when()
        .post("/api/audit-logs/{logId}/tags", log.getId())
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value());
  }

  private AuditLog recordedUpdate() {
    Passport root = passportRepository.findById(fixture.rootPassportId()).orElseThrow();
    AuditLog log = new AuditLog();
    log.setTargetType(AuditTargetType.PASSPORT);
    log.setTargetId(root.getId());
    log.setProjectId(fixture.projectId());
    log.setPassportId(root.getId());
    log.setAction(AuditLogAction.UPDATE);
    log.setDiff(
        objectMapper.readTree("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Wall A\"}]"));
    log.setCreatedById(fixture.apiKeyId());
    return auditLogRepository.save(log);
  }
}
