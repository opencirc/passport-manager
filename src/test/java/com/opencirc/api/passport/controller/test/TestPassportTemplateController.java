package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.model.PassportTemplate;
import io.github.thibaultmeyer.cuid.CUID;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TestPassportTemplateController {
  @Autowired
  @Qualifier("testRestTemplate")
  private RestTemplate restTemplate;

  /** Assigns random port number in which application runs. */
  @LocalServerPort private int port;

  /** AuthUserDetailsService mock bean. */
  @MockitoBean private AuthUserDetailsService authUserDetailsService;

  /** AuthUserDetailsService mock bean. */
  @MockitoBean private AuthenticationManager authenticationManager;

  /** JWT token. */
  private String jwtToken = null;

  @Autowired private PassportRepository passportRepository;
  @Autowired private DatasheetRepository datasheetRepository;
  @Autowired private PassportDatasheetMappingRepository passportDatasheetMappingRepository;
  @Autowired private PassportTemplateRepository passportTemplateRepository;
  @Autowired private ObjectMapper objectMapper;

  /** Active passport created for each test, with one generic datasheet. */
  private String fixturePassportId = null;

  /** Template persisted for each test. */
  private String persistedTemplateId = null;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("bsdd.class.details.url", () -> "http://localhost:8089" + "/api/Class/v1");
    registry.add(
        "bsdd.propertiesWithDetail.url", () -> "http://localhost:8089" + "/api/Property/v4");
  }

  /**
   * Sets up necessary configurations and mocks before each test execution. Initializes REST Assured
   * with the test server port, opens Mockito annotations, mocks the authentication context,
   * generates a mock JWT token, and stubs the bsdd API response.
   */
  @BeforeEach
  void setPortsAndMocks(WireMockRuntimeInfo wmInfo) {
    RestAssured.port = port;
    MockitoAnnotations.openMocks(this);
    // Mock auth
    MockAuthenticationTestHelper helper = new MockAuthenticationTestHelper();
    helper.mockUserDetails(authUserDetailsService, authenticationManager);
    generateMockJwtToken();
    createPassportAndTemplateFixtures();
  }

  /** Creates an active passport with a generic datasheet, plus one persisted template. */
  private void createPassportAndTemplateFixtures() {
    CreatedByDto createdBy = new CreatedByDto("User One", "user1@test.com");
    Map<String, Object> datasheetData =
        Map.of(
            "name",
            "Space for human dwelling",
            "classProperties",
            List.of(Map.of("name", "Handicap Accessible", "actualValue", "true")));

    Passport passport = new Passport();
    passport.setId(CUID.randomCUID2(24).toString());
    passport.setName("Template fixture passport");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setCreatedBy(createdBy);
    passport = passportRepository.save(passport);
    fixturePassportId = passport.getId();

    Datasheet datasheet = new Datasheet();
    datasheet.setName("Space for human dwelling");
    datasheet.setPlatform(Platform.BSDD);
    datasheet.setDataCategory(Datasheet.DataCategory.GENERIC);
    datasheet.setData(datasheetData);
    datasheet.setCreatedBy(createdBy);
    datasheet = datasheetRepository.save(datasheet);

    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setPassport(passport);
    mapping.setDatasheet(datasheet);
    passportDatasheetMappingRepository.save(mapping);

    Map<String, Object> templateData =
        Map.of(
            "name",
            "Space for human dwelling",
            "classProperties",
            List.of(Map.of("name", "Handicap Accessible", "actualValue", "")));
    PassportTemplate template =
        PassportTemplate.builder()
            .name("fixture template")
            .template(objectMapper.valueToTree(templateData))
            .createdById("87510a3c-4357-47bc-80a1-9ed02285fbae")
            .createdBy(createdBy)
            .build();
    persistedTemplateId = passportTemplateRepository.save(template).getId();
  }

  private void generateMockJwtToken() {
    String requestBody = "{\"email\": \"user\", \"password\": \"user1password\"}";
    Response response =
        given().contentType(ContentType.JSON).body(requestBody).when().post("/api/auth/login");
    if (response.getStatusCode() == 200) {
      jwtToken = response.getCookie("access_token");
    } else {
      throw new AssertionError("Expected status 200, but got " + response.getStatusCode());
    }
  }

  /** Tests successful creation of a passport template from an existing passport. */
  @Test
  public void shouldGenerateTemplateFromPassportSuccessfullyWithoutSaving() throws Exception {
    String templateName = "testtemplate";
    boolean dryRun = true;

    String passportId = fixturePassportId;

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .queryParam("dryRun", dryRun)
            .pathParam("passportId", passportId)
            .body(templateName)
            .when()
            .post("/api/passportTemplate/{passportId}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    response
        .then()
        .body("template.name", equalTo("Space for human dwelling"))
        .body("template.classProperties[0].actualValue", equalTo(""));
  }

  /**
   * Tests failure case when trying to create a template from a invalid passport. Verifies that the
   * API returns HTTP 404 when an invalid passport ID is passed.
   */
  @Test
  public void shouldReturnNotFoundWhenPassportNotFound() {
    String passportId = "invalid-id";
    boolean dryRun = true;
    String templateName = "My Template";

    Response response =
        RestAssured.given()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .queryParam("dryRun", dryRun)
            .body(templateName)
            .pathParam("passportId", passportId)
            .when()
            .post("/api/passportTemplate/{passportId}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .extract()
            .response();

    assertTrue(response.getBody().asString().contains("Active passport not found"));
  }

  /**
   * Tests successful retrieval of a persisted passport template. Ensures that the template is
   * returned correctly for a valid template ID.
   */
  @Test
  public void shouldRetrieveTemplateSuccessfullyById() {
    String templateId = persistedTemplateId;

    Response response =
        RestAssured.given()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .pathParam("templateId", templateId)
            .when()
            .get("/api/passportTemplate/{templateId}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    assertTrue(response.getBody().asString().contains(templateId));

    response
        .then()
        .body("id", equalTo(templateId))
        .body("template.name", equalTo("Space for human dwelling"))
        .body("template.classProperties[0].actualValue", equalTo(""));
  }

  /** Tests failure to fetch a template that does not exist. Ensures that HTTP 404. */
  @Test
  public void shouldReturnNotFoundForInvalidTemplateId() {
    String templateId = "00000000-0000-0000-0000-00000000000";

    Response response =
        RestAssured.given()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .pathParam("templateId", templateId)
            .when()
            .get("/api/passportTemplate/{templateId}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .log()
            .all()
            .extract()
            .response();

    assertTrue(
        response
            .getBody()
            .asString()
            .contains(
                "Passport template not found for ID: " + "00000000-0000-0000-0000-00000000000"));
  }

  /**
   * Tests retrieval of all persisted passport templates. Ensures that a list of templates is
   * returned and is not empty.
   */
  @Test
  public void shouldListAllPersistedTemplates() {
    Response response =
        RestAssured.given()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .when()
            .get("/api/passportTemplate/all")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    List<Map<String, Object>> templates = response.jsonPath().getList("$");
    assertNotNull(templates);
    assertFalse(templates.isEmpty());
  }
}
