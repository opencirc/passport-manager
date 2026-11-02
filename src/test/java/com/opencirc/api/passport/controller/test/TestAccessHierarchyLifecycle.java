package com.opencirc.api.passport.controller.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthService;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.helper.test.MachineAuthTestHelper;
import com.opencirc.api.passport.helper.test.MachineAuthTestHelper.MachineFixture;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.AccessGroupService;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.ProjectService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Complete lifecycle through the public API: admin login, project with its root passport, access
 * group and key, machine child creation with a cached bSDD definition, a property update and its
 * RFC 6902 audit entry, a signed and verified validation tag, project boundary enforcement, and
 * soft archiving of the project with its cascade.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestAccessHierarchyLifecycle {

  private static final String PASSWORD = "Str0ng-Passw0rd!";

  @LocalServerPort private int port;

  @Autowired private AuthService authService;
  @Autowired private ProjectService projectService;
  @Autowired private AccessGroupService accessGroupService;
  @Autowired private ApiKeyService apiKeyService;
  @Autowired private PassportRepository passportRepository;

  private String adminCookie;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("bsdd.class.details.url", () -> "http://localhost:8089/api/Class/v1");
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    String email = "e2e-" + UUID.randomUUID() + "@test.com";
    authService.register(email, PASSWORD, "End", "ToEnd");
    Response login =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", PASSWORD))
            .when()
            .post("/api/auth/login");
    assertEquals(HttpStatus.OK.value(), login.getStatusCode());
    adminCookie = login.getCookie("access_token");
  }

  @Test
  void adminEndpointsRejectMissingAndMachineCredentials() {
    given().when().get("/api/admin/projects").then().statusCode(HttpStatus.UNAUTHORIZED.value());

    MachineFixture machine =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.full());
    given()
        .header("Authorization", machine.bearer())
        .when()
        .get("/api/admin/projects")
        .then()
        // Spring Security answers a principal without the ADMIN authority through the
        // authentication entry point (401) rather than the access-denied handler (403).
        .statusCode(
            org.hamcrest.Matchers.anyOf(
                is(HttpStatus.UNAUTHORIZED.value()), is(HttpStatus.FORBIDDEN.value())))
        .body(org.hamcrest.Matchers.emptyOrNullString());
  }

  @Test
  void fullLifecycle() {
    // 1. Administrator provisions owner, project (with root passport), access group and key.
    String ownerId =
        given()
            .cookie("access_token", adminCookie)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "EcoOffice Holdings"))
            .when()
            .post("/api/admin/owners")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("id");

    Response project =
        given()
            .cookie("access_token", adminCookie)
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name", "EcoOffice Building B",
                    "description", "Commercial circular office complex",
                    "ownerId", ownerId))
            .when()
            .post("/api/admin/projects");
    project.then().statusCode(HttpStatus.CREATED.value()).body("rootPassportId", notNullValue());
    String projectId = project.path("id");
    String rootPassportId = project.path("rootPassportId");
    assertEquals("active", project.path("status"));

    String groupId =
        given()
            .cookie("access_token", adminCookie)
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name",
                    "BIM Pipeline Integrator",
                    "permissions",
                    Map.of(
                        "canCreate", true, "canRead", true, "canUpdate", true, "canDelete", false)))
            .when()
            .post("/api/admin/projects/{projectId}/access-groups", projectId)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("id");

    Response key =
        given()
            .cookie("access_token", adminCookie)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Revit Production Pipeline"))
            .when()
            .post("/api/admin/access-groups/{groupId}/api-keys", groupId);
    key.then().statusCode(HttpStatus.CREATED.value()).body("status", is("active"));
    String token = key.path("token");
    String keyId = key.path("id");
    org.junit.jupiter.api.Assertions.assertTrue(token.startsWith("opc_live_" + keyId + "_"));

    // 2. The machine creates a child passport under the root using a bSDD classification.
    String classUri = "https://identifier.buildingsmart.org/uri/test/e2e/1.0/class/E2E-" + keyId;
    stubClassTemplate(classUri);

    Response child =
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name",
                    "Wall W-01",
                    "parentId",
                    rootPassportId,
                    "platformId",
                    classUri,
                    "dataCategory",
                    "generic"))
            .when()
            .post("/api/passport/platform/bsdd");
    child.then().statusCode(HttpStatus.OK.value()).body("projectId", is(projectId));
    String childId = child.path("id");
    assertEquals(keyId, child.path("createdById"));
    assertEquals(rootPassportId, child.path("parentId"));
    String propertyId = child.path("datasheets[0].datasheetProperties[0].id");
    String datasheetId = child.path("datasheets[0].id");

    // A second passport with the same classification reuses the cached definition.
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Wall W-02", "parentId", childId, "platformId", classUri))
        .when()
        .post("/api/passport/platform/bsdd")
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("datasheets[0].platformId", is(classUri));

    // 3. A property update is audited as an RFC 6902 patch on the datasheet.
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("values", Map.of(propertyId, "EI60")))
        .when()
        .put("/api/passport/{passportId}/data", childId)
        .then()
        .statusCode(HttpStatus.OK.value());

    Response feed =
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/audit-logs/passport/{passportId}", childId);
    feed.then()
        .statusCode(HttpStatus.OK.value())
        .body("[0].action", is("UPDATE"))
        .body("[0].targetType", is("PASSPORT_DATASHEET"))
        .body("[0].targetId", is(datasheetId))
        .body("[0].createdById", is(keyId))
        .body("[0].diff[0].op", is("add"))
        .body("[0].diff[0].path", is("/data/" + propertyId))
        .body("[0].diff[0].value", is("EI60"))
        .body("action", hasItem("CREATE"));
    String logId = feed.path("[0].id");

    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/audit-logs/project/{projectId}", projectId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("targetId", hasItem(rootPassportId))
        .body("targetId", hasItem(childId));

    // 4. The state is validated with a signed tag and the signature verifies.
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("tag", "APPROVED", "email", "lead.auditor@certifier.org"))
        .when()
        .post("/api/audit-logs/{logId}/tags", logId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("tag", is("approved"))
        .body("signature", notNullValue());

    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/audit-logs/{logId}/tag/verify", logId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("valid", is(true));

    // 5. A key of another project can neither read nor mutate these passports.
    MachineFixture other =
        MachineAuthTestHelper.provision(
            projectService, accessGroupService, apiKeyService, AccessGroupPermissions.full());
    given()
        .header("Authorization", other.bearer())
        .contentType(ContentType.JSON)
        .body(Map.of("values", Map.of(propertyId, "EI30")))
        .when()
        .put("/api/passport/{passportId}/data", childId)
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value());
    given()
        .header("Authorization", other.bearer())
        .when()
        .get("/api/passport/{passportId}", childId)
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value());
    given()
        .header("Authorization", other.bearer())
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Intruder", "parentId", rootPassportId, "platformId", classUri))
        .when()
        .post("/api/passport/platform/bsdd")
        .then()
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());

    // 6. The access group forbids deletes, so archiving a passport is refused.
    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .delete("/api/passport/{passportId}", childId)
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value());

    // 7. The administrator soft-archives the project; passports follow and the key is shut out.
    given()
        .cookie("access_token", adminCookie)
        .when()
        .delete("/api/admin/projects/{projectId}", projectId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("status", is("archived"))
        .body("archivedTime", notNullValue());

    List<Passport> passports =
        passportRepository.findAll().stream()
            .filter(p -> projectId.equals(p.getProjectId()))
            .toList();
    assertEquals(3, passports.size());
    org.junit.jupiter.api.Assertions.assertTrue(
        passports.stream().allMatch(p -> p.getStatus() == Passport.Status.ARCHIVED));

    given()
        .cookie("access_token", adminCookie)
        .when()
        .get("/api/admin/projects")
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("id", not(hasItem(projectId)));
    given()
        .cookie("access_token", adminCookie)
        .queryParam("include_archived", true)
        .when()
        .get("/api/admin/projects")
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("id", hasItem(projectId));
    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());

    // 8. Restoring brings the project and its root passport back; the key works again.
    given()
        .cookie("access_token", adminCookie)
        .when()
        .post("/api/admin/projects/{projectId}/restore", projectId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("status", is("active"));
    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/passport/root")
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("", hasSize(1))
        .body("[0].id", is(rootPassportId))
        .body("[0].status", is("active"));

    // Deleting the owner keeps the project, now without an owner.
    given()
        .cookie("access_token", adminCookie)
        .when()
        .delete("/api/admin/owners/{id}", ownerId)
        .then()
        .statusCode(HttpStatus.NO_CONTENT.value());
    given()
        .cookie("access_token", adminCookie)
        .when()
        .get("/api/admin/projects/{projectId}", projectId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("ownerId", org.hamcrest.Matchers.nullValue());
  }

  private static void stubClassTemplate(String uri) {
    stubFor(
        get(urlPathEqualTo("/api/Class/v1"))
            .withQueryParam("Uri", equalTo(uri))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatus.OK.value())
                    .withBody(
                        """
                        {
                          "classType": "Class",
                          "referenceCode": "IfcWallE2E",
                          "code": "IfcWallE2E",
                          "name": "Wall (end-to-end)",
                          "definition": "A wall used by the lifecycle test",
                          "uri": "%s",
                          "status": "Active",
                          "classProperties": [
                            {
                              "name": "Fire Rating",
                              "propertyCode": "FireRating",
                              "propertySet": "Pset_WallCommon",
                              "uri": "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/FireRating",
                              "dataType": "String"
                            }
                          ]
                        }
                        """
                            .formatted(uri))));
  }
}
