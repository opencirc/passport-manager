package com.opencirc.api.passport.controller.test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.dao.DatasheetPropertyRepository;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import io.github.thibaultmeyer.cuid.CUID;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestEpdEnrichment {

  @LocalServerPort private int port;

  @Autowired private PassportRepository passportRepository;

  @Autowired private DatasheetRepository datasheetRepository;

  @Autowired private DatasheetPropertyRepository datasheetPropertyRepository;

  @Autowired private PassportDatasheetMappingRepository passportDatasheetMappingRepository;

  @MockBean private AuthUserDetailsService authUserDetailsService;

  @MockBean private AuthenticationManager authenticationManager;

  private String jwtToken;

  private static final String TRIGGER_CODE = "referencetooriginalEPD";
  private static final String TRIGGER_GROUP = "GeneralInformation";

  private static final String NAME_CODE = "productname";
  private static final String NAME_GROUP = "ProductInformation";

  @BeforeEach
  void setup(WireMockRuntimeInfo wmInfo) {
    RestAssured.port = port;
    MockitoAnnotations.openMocks(this);
    MockAuthenticationTestHelper helper = new MockAuthenticationTestHelper();
    helper.mockUserDetails(authUserDetailsService, authenticationManager);
    generateMockJwtToken();
  }

  private void generateMockJwtToken() {
    String requestBody = "{\"email\": \"user\", \"password\": \"user1password\"}";
    Response response =
        given().contentType(ContentType.JSON).body(requestBody).when().post("/api/auth/login");
    if (response.getStatusCode() == 200) {
      jwtToken = response.getCookie("access_token");
    } else {
      jwtToken = "mock-token";
    }
  }

  @Test
  public void shouldEnrichEPDDataWhenTriggerFieldIsUpdated() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");

    // Create a test passport
    Passport passport = new Passport();
    passport.setId(CUID.randomCUID2(24).toString());
    passport.setName("Test Passport for EPD");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setCreatedBy(createdBy);
    passport = passportRepository.save(passport);

    // Create a datasheet
    Datasheet datasheet = new Datasheet();
    datasheet.setName("Test Datasheet");
    datasheet.setPlatform(Platform.BSDD);
    datasheet.setDataCategory(Datasheet.DataCategory.GENERIC);
    datasheet.setCreatedBy(createdBy);
    datasheet = datasheetRepository.save(datasheet);

    // Map datasheet to passport
    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setPassport(passport);
    mapping.setDatasheet(datasheet);
    passportDatasheetMappingRepository.save(mapping);

    // Reload passport to have mappings
    passport = passportRepository.findById(passport.getId()).get();

    // Setup properties
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty nameProp = createProperty(datasheet, NAME_CODE, NAME_GROUP);

    // Stub WireMock
    stubFor(
        get(urlEqualTo("/epd-data.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{ \"processInformation\": { \"dataSetInformation\": { \"name\": { \"baseName\": \"Enriched Product\" } } } }")));

    // Update trigger field
    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/epd-data.json");
    UpdateDataRequestDto request = new UpdateDataRequestDto(values);

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    // Since it's async, we might need to wait a bit
    Thread.sleep(2000);

    // Verify that the product name was updated in the database
    Datasheet updatedDatasheet = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(updatedDatasheet.getData(), notNullValue());
    assertThat(updatedDatasheet.getData().get(nameProp.getId()), is("Enriched Product"));
  }

  @Test
  public void shouldHandleInvalidUrlGracefully() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);
    Datasheet datasheet = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, datasheet);
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);

    // Stub WireMock for a 404
    stubFor(get(urlEqualTo("/not-found.json")).willReturn(aResponse().withStatus(404)));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/not-found.json");
    UpdateDataRequestDto request = new UpdateDataRequestDto(values);

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(1000);
    // No exception thrown to user, and no data changed (except the URL itself)
    Datasheet updatedDatasheet = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(
        updatedDatasheet.getData().get(triggerProp.getId()),
        is("http://localhost:8089/not-found.json"));
  }

  @Test
  public void shouldEnrichMultipleDatasheets() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);

    Datasheet ds1 = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, ds1);
    DatasheetProperty triggerProp = createProperty(ds1, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty nameProp1 = createProperty(ds1, NAME_CODE, NAME_GROUP);

    Datasheet ds2 = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, ds2);
    DatasheetProperty nameProp2 = createProperty(ds2, NAME_CODE, NAME_GROUP);

    stubFor(
        get(urlEqualTo("/multi.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{ \"processInformation\": { \"dataSetInformation\": { \"name\": { \"baseName\": \"Multi Enriched\" } } } }")));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/multi.json");

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(new UpdateDataRequestDto(values))
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(2000);

    assertThat(
        datasheetRepository.findById(ds1.getId()).get().getData().get(nameProp1.getId()),
        is("Multi Enriched"));
    assertThat(
        datasheetRepository.findById(ds2.getId()).get().getData().get(nameProp2.getId()),
        is("Multi Enriched"));
  }

  @Test
  public void shouldEnrichMetadataEvenIfGWPIsMissing() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);
    Datasheet datasheet = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, datasheet);
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty nameProp = createProperty(datasheet, NAME_CODE, NAME_GROUP);
    DatasheetProperty gwpProp =
        createProperty(datasheet, "ClimateChangePerUnit", "Pset_EnvironmentalImpactIndicators");

    // JSON without GWP
    stubFor(
        get(urlEqualTo("/missing-gwp.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{ \"processInformation\": { \"dataSetInformation\": { \"name\": { \"baseName\": \"No GWP Product\" } } }, \"LCIAResults\": { \"LCIAResult\": [] } }")));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/missing-gwp.json");

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(new UpdateDataRequestDto(values))
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(2000);

    Datasheet updated = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(updated.getData().get(nameProp.getId()), is("No GWP Product"));
    assertThat(updated.getData().get(gwpProp.getId()), nullValue());
  }

  @Test
  public void shouldExtractPublicationDateFromNewPath() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);
    Datasheet datasheet = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, datasheet);
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty pubDateProp = createProperty(datasheet, "publicationdateofEPD", "Date");

    String json =
        """
        {
          "processInformation": {
            "time": {
              "other": {
                "anies": [
                  {
                    "value": "2024-05-05"
                  }
                ]
              }
            }
          }
        }
        """;

    stubFor(
        get(urlEqualTo("/new-path.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/new-path.json");

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(new UpdateDataRequestDto(values))
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(2000);

    Datasheet updated = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(updated.getData().get(pubDateProp.getId()), is("2024-05-05"));
  }

  @Test
  public void shouldExtractGwpFromNewPath() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);
    Datasheet datasheet = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, datasheet);
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty gwpProp =
        createProperty(datasheet, "ClimateChangePerUnit", "Pset_EnvironmentalImpactIndicators");

    String json =
        """
        {
          "LCIAResults": {
            "LCIAResult": [
              {
                "referenceToLCIAMethodFlowProperty": {
                  "refObjectId": "a7ea142a-9749-11ed-a8fc-0242ac120002"
                },
                "other": {
                  "anies": [
                    {
                      "module": "A1-A3",
                      "value": "123.45"
                    },
                    {
                      "module": "C1",
                      "value": "0.1"
                    }
                  ]
                }
              }
            ]
          }
        }
        """;

    stubFor(
        get(urlEqualTo("/new-gwp-path.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/new-gwp-path.json");

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(new UpdateDataRequestDto(values))
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(2000);

    Datasheet updated = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(updated.getData().get(gwpProp.getId()), is("123.45"));
  }

  @Test
  public void shouldEnrichLCAxData() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);
    Datasheet datasheet = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, datasheet);
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty nameProp = createProperty(datasheet, "productname", "ProductInformation");
    DatasheetProperty gwpProp =
        createProperty(datasheet, "ClimateChangePerUnit", "Pset_EnvironmentalImpactIndicators");

    String json =
        """
        {
          "name": "LCAx Product",
          "format": "LCAx",
          "impacts": {
            "gwp": {
              "a1a3": 45.67
            }
          }
        }
        """;

    stubFor(
        get(urlPathEqualTo("/lcax-epd.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/lcax-epd.json");

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(new UpdateDataRequestDto(values))
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(2000);

    Datasheet updated = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(updated.getData().get(nameProp.getId()), is("LCAx Product"));
    assertThat(updated.getData().get(gwpProp.getId()).toString(), containsString("45.67"));
  }

  @Test
  public void shouldEnrichLCAxDataWithAllFields() throws InterruptedException {
    CreatedByDto createdBy = new CreatedByDto("System", "system@opencirc.org");
    Passport passport = createPassport(createdBy);
    Datasheet datasheet = createDatasheet(createdBy);
    mapDatasheetToPassport(passport, datasheet);
    DatasheetProperty triggerProp = createProperty(datasheet, TRIGGER_CODE, TRIGGER_GROUP);
    DatasheetProperty nameProp = createProperty(datasheet, "productname", "ProductInformation");
    DatasheetProperty pubDateProp = createProperty(datasheet, "publicationdateofEPD", "Date");
    DatasheetProperty validUntilProp = createProperty(datasheet, "datasetvaliduntil", "Date");
    DatasheetProperty serviceLifeProp =
        createProperty(
            datasheet, "referenceservicelifeaccordingtoISO15686-8", "ProductInformation");
    DatasheetProperty ownerProp = createProperty(datasheet, "nameofowner", "PartiesInvolved");
    DatasheetProperty unitProp =
        createProperty(datasheet, "referenceunittype", "ReferenceUnitType");
    DatasheetProperty gwpProp =
        createProperty(datasheet, "ClimateChangePerUnit", "Pset_EnvironmentalImpactIndicators");

    String json =
        """
        {
          "name": "Full LCAx Product",
          "format": "LCAx",
          "publishedDate": "2024-01-01",
          "validUntil": "2029-01-01",
          "referenceServiceLife": 50,
          "source": {
            "name": "LCAx Owner"
          },
          "declaredUnit": "M2",
          "impacts": {
            "gwp": {
              "a1a3": 12.34
            }
          }
        }
        """;

    stubFor(
        get(urlPathEqualTo("/full-lcax-epd.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

    Map<String, Object> values = new HashMap<>();
    values.put(triggerProp.getId(), "http://localhost:8089/full-lcax-epd.json");

    given()
        .cookie("access_token", jwtToken)
        .contentType(ContentType.JSON)
        .body(new UpdateDataRequestDto(values))
        .when()
        .put("/api/passport/" + passport.getId() + "/data")
        .then()
        .statusCode(HttpStatus.OK.value());

    Thread.sleep(2000);

    Datasheet updated = datasheetRepository.findById(datasheet.getId()).get();
    assertThat(updated.getData().get(nameProp.getId()), is("Full LCAx Product"));
    assertThat(updated.getData().get(pubDateProp.getId()), is("2024-01-01"));
    assertThat(updated.getData().get(validUntilProp.getId()), is("2029-01-01"));
    assertThat(updated.getData().get(serviceLifeProp.getId()), is("50"));
    assertThat(updated.getData().get(ownerProp.getId()), is("LCAx Owner"));
    assertThat(updated.getData().get(unitProp.getId()), is("M2"));
    assertThat(updated.getData().get(gwpProp.getId()).toString(), containsString("12.34"));
  }

  private Passport createPassport(CreatedByDto createdBy) {
    Passport passport = new Passport();
    passport.setId(CUID.randomCUID2(24).toString());
    passport.setName("Test Passport");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setCreatedBy(createdBy);
    return passportRepository.save(passport);
  }

  private Datasheet createDatasheet(CreatedByDto createdBy) {
    Datasheet datasheet = new Datasheet();
    datasheet.setName("Test Datasheet");
    datasheet.setPlatform(Platform.BSDD);
    datasheet.setDataCategory(Datasheet.DataCategory.GENERIC);
    datasheet.setCreatedBy(createdBy);
    return datasheetRepository.save(datasheet);
  }

  private void mapDatasheetToPassport(Passport passport, Datasheet datasheet) {
    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setPassport(passport);
    mapping.setDatasheet(datasheet);
    passportDatasheetMappingRepository.save(mapping);
    // Reload to ensure mappings are present
    passport.getDatasheetMappings().add(mapping);
  }

  private DatasheetProperty createProperty(Datasheet datasheet, String code, String groupTag) {
    DatasheetProperty prop = new DatasheetProperty();
    prop.setDatasheet(datasheet);
    prop.setCode(code);
    prop.setGroupTag(groupTag);
    return datasheetPropertyRepository.save(prop);
  }
}
