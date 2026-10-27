package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.helper.test.BsddMockStubHelper;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.helper.test.TestConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestDataDictionaryController {

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
    String requestBody = "{\"email\": \"user\", \"password\": \"user1password\"}";
    Response response =
        given().contentType(ContentType.JSON).body(requestBody).when().post("/api/auth/login");
    if (response.getStatusCode() == 200) {
      jwtToken = response.getCookie("access_token");
    } else {
      throw new AssertionError("Expected status 200, but got " + response.getStatusCode());
    }
  }

  /**
   * Tests the class search API endpoint to ensure it returns the expected class information when a
   * valid dictionary and class code query are provided.
   */
  @Test
  public void shouldReturnMatchingClassWhenSearchQueryIsValid() {
    String dictionary = "bsdd";
    String query = "EC004131";

    BsddMockStubHelper.stubSearchClassApiResponse();

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .when()
            .get("/api/dataDictionary/{dictionary}/class/search/{query}", dictionary, query)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    List<Map<String, String>> classList = response.jsonPath().getList("$");
    assertFalse(classList.isEmpty());
    assertEquals("Storage container for hazardous material", classList.get(0).get("name"));
    assertEquals("EC004131", classList.get(0).get("code"));
    assertEquals(
        "https://identifier.buildingsmart.org/uri/etim/etim/8.0/class/EC004131",
        classList.get(0).get("uri"));
  }

  /**
   * Tests the property search API endpoint to ensure it returns matching properties when provided
   * with a valid dictionary and query string.
   */
  @Test
  public void shouldReturnMatchingPropertiesWhenQueryIsValid() {
    String dictionary = "bsdd";
    String query = "temperature";
    BsddMockStubHelper.stubListPropertiesApiResponse();

    Response response =
        RestAssured.given()
            .log()
            .all()
            .cookie("access_token", jwtToken)
            .contentType(ContentType.JSON)
            .when()
            .get("/api/dataDictionary/{dictionary}/property/search/{query}", dictionary, query)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .log()
            .all()
            .extract()
            .response();

    response
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(ContentType.JSON)
        .body("[0].code", equalTo("ApplicationTemperature"))
        .body("[0].name", equalTo("ApplicationTemperature"))
        .body(
            "[0].uri",
            equalTo(
                "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/ApplicationTemperature"))
        .body("[1].code", equalTo("ActivationTemperature"))
        .body("[1].name", equalTo("Activation Temperature"))
        .body(
            "[1].uri",
            equalTo(
                "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/ActivationTemperature"));
  }
}
