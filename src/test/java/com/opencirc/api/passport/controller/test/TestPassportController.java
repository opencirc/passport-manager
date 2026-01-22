package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Integration tests for the passport controller. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TestPassportController {

  @LocalServerPort private int port;

  @MockBean private AuthUserDetailsService authUserDetailsService;

  @MockBean private AuthenticationManager authenticationManager;

  @Autowired private ObjectMapper objectMapper;

  /** JWT token. */
  private String jwtToken = null;

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
  }

  private void generateMockJwtToken() {
    String requestBody = "{\"username\": \"user1\", \"password\": \"user1password\"}";
    Response response =
        given().contentType(ContentType.JSON).body(requestBody).when().post("/api/auth/login");
    if (response.getStatusCode() == 200) {
      jwtToken = response.getCookie("access_token");
    } else {
      throw new AssertionError("Expected status 200, but got " + response.getStatusCode());
    }
  }

  /** Tests the successful creation of a passport with valid JSON input. */
  @Test
  public void givenValidBsddDataWhenCreatePassportThenReturnCreatedPassportWithCorrectDetails()
      throws JsonProcessingException {

    var createdByDto = new CreatedByDto();
    createdByDto.setFullName("User One");
    createdByDto.setEmail("user1@test.com");

    String jsonBody =
        """
                                {
                    "classType": "Class",
                    "referenceCode": "A-A__",
                    "relatedIfcEntityNames": [
                        "IfcSpace"
                    ],
                    "parentClassReference": {
                        "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/uocs",
                        "name": "Use of Construction Spaces",
                        "code": "uocs"
                    },
                    "classProperties": [
                        {
                            "dataType": "Boolean",
                            "name": "Handicap Accessible",
                            "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
                            "actualValue": "true"
                        }
                    ],
                    "definition": "space designed for human dwelling and related activities",
                    "name": "Space for human dwelling",
                    "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__",
                    "status": "Active",
                    "templateName": "testTemplate",
                    "dataCategory": "Unique"
                }
                """;

    CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
    requestDto.setCreatedTime(OffsetDateTime.now());
    requestDto.setDataCategory(DataCategory.GENERIC.getValue());
    requestDto.setDatasheetData(objectMapper.readTree(jsonBody));
    requestDto.setPassportName("Dwelling Space");
    requestDto.setCreatedBy(createdByDto);

    Platform platform = Platform.BSDD;
    DataDictionary dictionary = DataDictionary.TABLE6;

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .pathParam("platform", platform.getValue())
            .pathParam("dictionary", dictionary.getValue())
            .when()
            .post("/api/passport/dictionary/{platform}/{dictionary}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    response
        .then()
        .body("name", equalTo("Dwelling Space"))
        .body("datasheets[0].data.classType", equalTo("Class"))
        .body("datasheets[0].data.referenceCode", equalTo("A-A__"))
        .body("datasheets[0].data.relatedIfcEntityNames[0]", equalTo("IfcSpace"))
        .body("datasheets[0].data.classProperties[0].name", equalTo("Handicap Accessible"))
        .body("datasheets[0].data.classProperties[0].actualValue", equalTo("true"))
        .body(
            "datasheets[0].data.definition",
            equalTo("space designed for human dwelling and related activities"));
  }

  /** Tests the behavior of the createPassport method when an empty JSON body is provided. */
  @Test
  public void shouldFailToCreatePassportWhenJsonBodyIsEmpty() throws JsonValidationException {
    var createdByDto = new CreatedByDto();
    createdByDto.setFullName("User One");
    createdByDto.setEmail("user1@test.com");

    CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
    requestDto.setCreatedTime(OffsetDateTime.now());
    requestDto.setCreatedBy(createdByDto);
    requestDto.setPassportName("Empty Passport");

    Platform platform = Platform.BSDD;
    DataDictionary dictionary = DataDictionary.TABLE6;

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .pathParam("platform", platform.getValue())
            .pathParam("dictionary", dictionary.getValue())
            .when()
            .post("/api/passport/dictionary/{platform}/{dictionary}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .log()
            .all()
            .extract()
            .response();

    String responseBody = response.getBody().asString();
    assertTrue(responseBody.contains("Input JSON node is null"));
  }

  /** Tests the behavior of the createPassport method when an invalid JSON body is provided. */
  @Test
  public void shouldFailToCreatePassportWhenJsonBodyIsInvalid() throws JsonProcessingException {
    var createdByDto = new CreatedByDto();
    createdByDto.setFullName("User One");
    createdByDto.setEmail("user1@test.com");

    JsonNode invalidNode =
        objectMapper.readTree(
            """
            {
                "ABCDFWEREWRHIH": "ABCDFWEREWRHIH"
            }
        """);

    CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
    requestDto.setCreatedTime(OffsetDateTime.now());
    requestDto.setCreatedBy(createdByDto);
    requestDto.setPassportName("Invalid Passport");
    requestDto.setDataCategory(DataCategory.GENERIC.getValue());
    requestDto.setDatasheetData(invalidNode);

    Platform platform = Platform.BSDD;
    DataDictionary dictionary = DataDictionary.TABLE6;

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .body(requestDto)
            .pathParam("platform", platform.getValue())
            .pathParam("dictionary", dictionary.getValue())
            .when()
            .post("/api/passport/dictionary/{platform}/{dictionary}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
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
   */
  @Test
  public void shouldReturnPassportDetailsWhenPassportIdIsValid() throws Exception {

    String passportId = "w6jqrmihmjcqf098dslae1ppsx3hdg7l4wgb";

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setName("Dwelling Space");
    PassportDatasheetMapping datasheetMapping = new PassportDatasheetMapping();
    Datasheet datasheet = new Datasheet();
    datasheet.setData(generateDatasheetData());
    datasheetMapping.setDatasheet(datasheet);
    datasheetMapping.setPassport(passport);
    datasheetMapping.setId("cd4d3ec2-0c8a-45bf-888f-81fdbd9eaa37");
    passport.setDatasheetMappings(Set.of(datasheetMapping));

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .pathParam("id", passportId)
            .when()
            .get("/api/passport/{id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    // Assertions
    response
        .then()
        .body("id", equalTo(passportId))
        .body("name", equalTo("Dwelling Space"))
        .body("datasheets[0].data.classType", equalTo("Class"))
        .body("datasheets[0].data.classProperties[0].name", equalTo("Handicap Accessible"))
        .body("datasheets[0].data.classProperties[0].actualValue", equalTo("true"))
        .body(
            "datasheets[0].data.definition",
            equalTo("space designed for human dwelling and related activities"));
  }

  /**
   * Tests retrieval of passport details along with its children by a valid passport ID.
   *
   * <p>Verifies that the response includes child passport objects, with correct parent-child
   * relationships and datasheet property values.
   */
  @Test
  public void shouldReturnPassportDetailsWithChildrenWhenPassportIdIsValid() throws Exception {

    String passportId = "w6jqrmihmjcqf098dslae1ppsx3hdg7l4wgb";

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .pathParam("id", passportId)
            .when()
            .get("/api/passport/{id}/children")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    // Assertions
    response
        .then()
        .body("[0].id", equalTo(passportId))
        .body("[0].datasheets[0].data.classProperties[0].name", equalTo("Handicap Accessible"))
        .body("[1].parent.id", equalTo(passportId))
        .body("[1].id", equalTo("i29r9y3zkyjqkek7wkzjvpk1zkyeq98g1t3t"));
  }

  private JsonNode generateDatasheetData() throws JsonMappingException, JsonProcessingException {

    String jsonBody =
        """
                {
            "id": "c98fbb40-5c9e-4daf-a2ae-961b1aa75adb",
            "data": {
                "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__",
                "code": "A-A__",
                "name": "Space for human dwelling",
                "status": "Active",
                "classType": "Class",
                "classProperties": [
                    {
                        "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
                        "code": "HandicapAccessible",
                        "name": "Handicap Accessible",
                        "status": "Active",
                        "dataType": "Boolean",
                        "definition": "Indication that this object is designed to be accessible by the handicapped.",
                        "actualValue": ""
                    }
                ]
            },
            "dataCategory": null,
            "dataDictionary": null,
            "createdBy": "abc@example.com",
            "createdTime": "2025-06-10T12:00:00"
        }
        """;

    return objectMapper.readTree(jsonBody);
  }
}
