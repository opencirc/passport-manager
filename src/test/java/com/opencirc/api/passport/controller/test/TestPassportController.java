package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.helper.test.MachineAuthTestHelper;
import com.opencirc.api.passport.helper.test.MachineAuthTestHelper.MachineFixture;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.service.AccessGroupService;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.PassportService;
import com.opencirc.api.passport.service.ProjectService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestPassportController {

  @LocalServerPort private int port;

  @MockitoBean private PassportService passportService;

  @Autowired private ProjectService projectService;

  @Autowired private AccessGroupService accessGroupService;

  @Autowired private ApiKeyService apiKeyService;

  private MachineFixture fixture;

  @BeforeEach
  void setPortsAndFixtures() {
    RestAssured.port = port;
    fixture =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.full());
  }

  @Test
  public void getImmediateChildren_ShouldReturnOk_WhenPassportExists() {
    String passportId = "test-id";
    PassportDto child = new PassportDto();
    child.setId("child-id");
    child.setName("Child Name");

    when(passportService.getImmediateChildren(eq(passportId), any())).thenReturn(List.of(child));

    given()
        .header("Authorization", fixture.bearer())
        .pathParam("passportId", passportId)
        .when()
        .get("/api/passport/{passportId}/immediateChildren")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(ContentType.JSON)
        .body("[0].id", equalTo("child-id"))
        .body("[0].name", equalTo("Child Name"));
  }

  @Test
  public void getImmediateChildren_ShouldReturnNotFound_WhenPassportDoesNotExist() {
    String passportId = "missing-id";
    when(passportService.getImmediateChildren(eq(passportId), any()))
        .thenThrow(
            new HttpServerErrorException(
                HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId));

    given()
        .header("Authorization", fixture.bearer())
        .pathParam("passportId", passportId)
        .when()
        .get("/api/passport/{passportId}/immediateChildren")
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value());
  }

  @Test
  public void shouldPassTheKeyScopedToItsProject() {
    when(passportService.getRootPassports(any())).thenReturn(List.of());

    given()
        .header("Authorization", fixture.bearer())
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.OK.value());

    ArgumentCaptor<ApiKeyPrincipal> actor = ArgumentCaptor.forClass(ApiKeyPrincipal.class);
    verify(passportService).getRootPassports(actor.capture());
    assertEquals(fixture.projectId(), actor.getValue().getProjectId());
    assertEquals(fixture.apiKeyId(), actor.getValue().getKeyId());
    assertEquals(fixture.accessGroupId(), actor.getValue().getAccessGroupId());
  }

  @Test
  public void shouldAcceptTokenInApiKeyHeader() {
    when(passportService.getRootPassports(any())).thenReturn(List.of());

    given()
        .header("X-Api-Key", fixture.token())
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  public void shouldRejectMissingCredentials() {
    given().when().get("/api/passport/root").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    verify(passportService, never()).getRootPassports(any());
  }

  @Test
  public void shouldRejectWrongSecret() {
    String tampered = fixture.token().substring(0, fixture.token().length() - 4) + "zzzz";

    given()
        .header("Authorization", "Bearer " + tampered)
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
    verify(passportService, never()).getRootPassports(any());
  }

  @Test
  public void shouldRejectRevokedKey() {
    apiKeyService.revokeApiKey(fixture.apiKeyId());

    given()
        .header("Authorization", fixture.bearer())
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
    verify(passportService, never()).getRootPassports(any());
  }

  @Test
  public void shouldRejectKeyOfArchivedProject() {
    projectService.archiveProject(fixture.projectId());

    given()
        .header("Authorization", fixture.bearer())
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  public void shouldForbidWritesWithoutCreatePermission() throws Exception {
    MachineFixture readOnly =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.readOnly());

    given()
        .header("Authorization", readOnly.bearer())
        .contentType(ContentType.JSON)
        .body("{\"platform\":\"bsdd\",\"platformId\":\"https://example.org/class/IfcWall\"}")
        .when()
        .post("/api/passport/{passportId}/datasheet", readOnly.rootPassportId())
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value());

    verify(passportService, never())
        .addDatasheetsToPassportUsingPlatform(any(), any(), any(), any(), any());
  }

  @Test
  public void shouldAllowReadsWithReadOnlyPermission() {
    MachineFixture readOnly =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.readOnly());
    when(passportService.getRootPassports(any())).thenReturn(List.of());

    given()
        .header("Authorization", readOnly.bearer())
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.OK.value());
  }
}
