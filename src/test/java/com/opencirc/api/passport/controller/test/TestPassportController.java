package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TestPassportController {

  /** Assigns random port number in which application runs. */
  @LocalServerPort private int port;

  /** AuthUserDetailsService mock bean. */
  @MockBean private AuthUserDetailsService authUserDetailsService;

  /** AuthUserDetailsService mock bean. */
  @MockBean private AuthenticationManager authenticationManager;

  /** Injecting ObjectMapper bean. */
  @Autowired private ObjectMapper objectMapper;

  /** JWT token. */
  private String jwtToken = null;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("bsdd.classDetails.url", () -> "http://localhost:8089" + "/api/Class/v1");
    registry.add(
        "bsdd.propertiesWithDetail.url", () -> "http://localhost:8089" + "/api/Property/v4");
  }

  /**
   * Sets up necessary configurations and mocks before each test execution. Initializes REST Assured
   * with the test server port, opens Mockito annotations, mocks the authentication context,
   * generates a mock JWT token, and stubs the bsdd API response.
   *
   * @param wmInfo
   */
  @BeforeEach
  void setPortsAndMocks(WireMockRuntimeInfo wmInfo) {
    RestAssured.port = port;
    MockitoAnnotations.openMocks(this);
    // Mock auth
    MockAuthenticationTestHelper helper = new MockAuthenticationTestHelper();
    helper.mockUserDetails(authUserDetailsService, authenticationManager);
    generateMockJwtToken();
  }

  private void generateMockJwtToken() {
    String requestBody = "{\"email\": \"admin@test.com\", \"password\": \"Password123!\"}";
    Response response =
        given().contentType(ContentType.JSON).body(requestBody).when().post("/api/auth/login");
    if (response.getStatusCode() == TestConstants.STATUS_SUCCESS) {
      jwtToken = response.getCookie("access_token");
    } else {
      throw new AssertionError("Expected status 200, but got " + response.getStatusCode());
    }
  }

  /**
   * Tests the successful creation of a passport with valid JSON input.
   *
   * @throws JsonProcessingException
   * @throws JsonMappingException
   */
  @Test
  public void givenValidBsddDataWhenCreatePassportThenReturnCreatedPassportWithCorrectDetails()
      throws JsonValidationException, JsonMappingException, JsonProcessingException {
    String dictionary = "ifc";
    String platform = "bsdd";
    String jsonBody =
        """
                               {
    "classType": "Class",
    "referenceCode": "",
    "relatedIfcEntityNames": [
        "IfcWasteTerminal"
    ],
    "parentClassReference": {
        "uri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/class/br18B2T6.5.2",
        "name": "Logistik",
        "code": "br18B2T6.5.2"
    },
    "classProperties": [
        {
            "name": "KlimapåvirkningBe18Tabel6",
            "uri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/class/br18B2T6.5.2.1/prop/Klimapåvirkning/br18P297stk4",
            "description": "BR 18, bilag 2, tabel 6 Bygningsdele til beregning af klimapåvirkning 10.01.2024,",
            "definition": "BR 18, bilag 2, tabel 6 Bygningsdele til beregning af klimapåvirkning 10.01.2024,",
            "dataType": "Boolean",
            "propertyCode": "br18P297stk4",
            "propertyDictionaryName": "Tabel6",
            "dictionaryUri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1",
            "propertyUri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/prop/br18P297stk4",
            "propertySet": "Klimapåvirkning",
            "status": "Active",
            "propertyValueKind": "Single",
            "actualValue": "true"
        }
    ],
    "dictionaryUri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1",
    "activationDateUtc": "9999-12-31T23:59:59Z",
    "code": "br18B2T6.5.2.1",
    "definition": "Affald er en del af Logistik som kategoriseres under: VVS-anlæg",
    "name": "Affald",
    "uri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/class/br18B2T6.5.2.1",
    "status": "Preview",
    "subdivisionsOfUse": [],
    "versionDateUtc": "2025-11-13T00:00:00Z"
}
                                """;

    CreatePassportRequestDto createPassportRequest = new CreatePassportRequestDto();
    createPassportRequest.setDataCategory(DataCategory.GENERIC.getValue());
    createPassportRequest.setDatasheetData(objectMapper.readTree(jsonBody));
    createPassportRequest.setPassportName("Dwelling Space");
    createPassportRequest.setCreatedBy(new CreatedByDto("test admin", "admin@test.com"));
    createPassportRequest.setCreatedById("717753dc-ba6c-4a8d-87c9-cce878986553");

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(createPassportRequest)
            .pathParam("dictionary", dictionary)
            .pathParam("platform", platform)
            .when()
            .post("/api/passport/dictionary/{platform}/{dictionary}")
            .then()
            .statusCode(HttpStatus.SC_SUCCESS)
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();
    response
        .then()
        .body("name", equalTo("Dwelling Space"))
        .body("datasheets[0].data.br18P297stk4", equalTo("true"));
  }

  /** Tests the behaviour of the createPassport method when an empty JSON body is provided. */
  @Test
  public void shouldFailToCreatePassportWhenJsonBodyIsEmpty() throws JsonValidationException {
    String dictionary = "ifc";
    String platform = "bsdd";
    CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
    requestDto.setCreatedBy(new CreatedByDto("test admin", "admin@test.com"));
    requestDto.setPassportName("Empty Passport");

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .pathParam("dictionary", dictionary)
            .pathParam("platform", platform)
            .when()
            .post("/api/passport/dictionary/{platform}/{dictionary}")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .log()
            .all()
            .extract()
            .response();

    String responseBody = response.getBody().asString();
    assertTrue(responseBody.contains("Input JSON node is null"));
  }

  /**
   * Tests the behaviour of the createPassport method when an invalid JSON body is provided.
   *
   * @throws JsonProcessingException
   * @throws JsonMappingException
   */
  @Test
  public void shouldFailToCreatePassportWhenJsonBodyIsInvalid()
      throws JsonValidationException, JsonMappingException, JsonProcessingException {
    String dictionary = "ifc";
    String platform = "bsdd";
    JsonNode invalidNode =
        objectMapper.readTree(
            """
            {
                "ABCDFWEREWRHIH": "ABCDFWEREWRHIH"
            }
        """);

    CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
    requestDto.setCreatedBy(new CreatedByDto("test admin", "admin@test.com"));
    requestDto.setPassportName("Invalid Passport");
    requestDto.setDataCategory(DataCategory.GENERIC.getValue());
    requestDto.setDatasheetData(invalidNode);

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .pathParam("dictionary", dictionary)
            .pathParam("platform", platform)
            .when()
            .post("/api/passport/dictionary/{platform}/{dictionary}")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .log()
            .all()
            .extract()
            .response();

    String responseBody = response.getBody().asString();
    assertTrue(responseBody.contains("Invalid Template"));
  }

  /**
   * Tests retrieval of passport details by a valid passport ID.Verifies that the response contains
   * the correct passport ID, name, and associated datasheet information.
   *
   * @throws Exception if the request or data processing fails
   */
  @Test
  public void shouldReturnPassportDetailsWhenPassportIdIsValid() throws Exception {

    String passportId = "w1yi7790bs0mutg7i8kumbv9t6pdrf83wqan";
    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .pathParam("passportId", passportId)
            .when()
            .get("/api/passport/{passportId}")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();
    // Assertions
    response
        .then()
        .body("id", equalTo(passportId))
        .body("name", equalTo("Passport1"))
        .body("datasheets[0].code", equalTo("br18B2T6.5.2"));
  }

  /**
   * Tests retrieval of passport details along with its children by a valid passport ID.
   *
   * <p>Verifies that the response includes child passport objects, with correct parent-child
   * relationships and datasheet property values.
   *
   * @throws Exception if the request or data processing fails
   */
  @Test
  public void shouldReturnPassportDetailsWithChildrenWhenPassportIdIsValid() throws Exception {

    String passportId = "w1yi7790bs0mutg7i8kumbv9t6pdrf83wqan";

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .pathParam("passportId", passportId)
            .when()
            .get("/api/passport/{passportId}/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    // Assertions
    response
        .then()
        .body("[0].id", equalTo(passportId))
        .body("[1].parentId", equalTo(passportId))
        .body("[1].id", equalTo("aanq3ejcczzbzk7x3kngi79mmkdl4ooigm8n"));
  }

  /**
   * Tests the successful updation of a property value in a passport with valid input.
   *
   * @throws JsonProcessingException
   * @throws JsonMappingException
   */
  @Test
  public void shouldUpdateData() throws Exception {

    String passportId = "w1yi7790bs0mutg7i8kumbv9t6pdrf83wqan";
    Map<String, Object> values = new HashMap<String, Object>();
    String randomValue = String.format("%.3f", Math.round((Math.random() * 10) * 1000.0) / 1000.0);
    values.put("EF000228", randomValue);
    UpdateDataRequestDto updateDataRequest = new UpdateDataRequestDto(values);

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(updateDataRequest)
            .pathParam("passportId", passportId)
            .when()
            .put("/api/passport/{passportId}/data")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();
    response
        .then()
        .body("id", equalTo(passportId))
        .body("datasheets[0].data.EF000228", equalTo(randomValue));
  }
}
