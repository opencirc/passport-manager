package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.helper.test.BsddMockStubHelper;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestDataDictionaryController {

    /**
     * Assigns random port number in which application runs.
     */
    @LocalServerPort
    private int port;

    /**
     * AuthUserDetailsService mock bean.
     */
    @MockBean
    private AuthUserDetailsService authUserDetailsService;

    /**
     * AuthUserDetailsService mock bean.
     */
    @MockBean
    private AuthenticationManager authenticationManager;

    /**
     * RestTemplate bean.
     */
    @Autowired
    @Qualifier("testRestTemplate")
    private RestTemplate restTemplate;

    /**
     * JWT token.
     */
    private String jwtToken = null;


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("bsDD.classDetails.url",
                () -> "http://localhost:8089" + "/api/Class/v1");
        registry.add("bsDD.propertiesWithDetail.url",
                () -> "http://localhost:8089" + "/api/Property/v4");
    }

    /**
     * Sets up necessary configurations and mocks before each test execution.
     * Initializes REST Assured with the test server port, opens Mockito
     * annotations, mocks the authentication context, generates a mock JWT token,
     * and stubs the bsDD API response.
     * @param wmInfo
     */
    @BeforeEach
    void setPortsAndMocks(WireMockRuntimeInfo wmInfo) {
        RestAssured.port = port;
        MockitoAnnotations.openMocks(this);
        // Mock auth
        MockAuthenticationTestHelper helper = new MockAuthenticationTestHelper();
        helper.mockUserDetailsDB(authUserDetailsService, authenticationManager);

        generateMockJwtToken();

    }

    private void generateMockJwtToken() {
        String requestBody = "{\"username\": \"user1\", \"password\": \"user1password\"}";
        Response response = given().contentType(ContentType.JSON).body(requestBody).when()
                .post("/api/auth/login");
        if (response.getStatusCode() == TestConstants.STATUS_SUCCESS) {
            jwtToken = response.getCookie("access_token");
        } else {
            throw new AssertionError(
                    "Expected status 200, but got " + response.getStatusCode());
        }

    }


    /**
     * Tests the class search API endpoint to ensure it returns the expected class
     * information when a valid dictionary and class code query are provided.
     */
    @Test
    public void shouldReturnMatchingClassWhenSearchQueryIsValid() {
        String dictionary = "bsdd";
        String query = "EC004131";

        BsddMockStubHelper.stubSearchClassApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/data-dictionary/{dictionary}/class/search/{query}", dictionary, query)
                .then()
                .statusCode(TestConstants.STATUS_SUCCESS)
                .contentType(ContentType.JSON)
                .log().all()
                .extract().response();

        List<Map<String, String>> classList = response.jsonPath().getList("$");
        assertFalse(classList.isEmpty());
        assertEquals("Storage container for hazardous material", classList.get(0).get("name"));
        assertEquals("EC004131", classList.get(0).get("code"));
        assertEquals("https://identifier.buildingsmart.org/uri/etim/etim/8.0/class/EC004131", classList.get(0).get("uri"));
    }



    /**
     * Tests the functionality of fetching data from the bsDD API.
     *
     * Verifies that the data retrieval works correctly and the expected response is
     * returned when the bsDD API is called with valid parameters.
     */
    @Test
    public void shouldReturnClassWithPropertiesWhenUriIsValid() throws JsonValidationException {
        String classUri = "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__";
        String dictionary = "bsdd";
        boolean withProperties = true;
        BsddMockStubHelper.stubGetClassApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .queryParam("withProperties", withProperties)
                .body(classUri)
                .when()
                .post("/api/data-dictionary/{dictionary}/class", dictionary)
                .then()
                .statusCode(TestConstants.STATUS_SUCCESS)
                .contentType(ContentType.JSON)
                .log().all()
                .extract().response();

        String json = response.getBody().asString();
        assertTrue(json.contains("\"name\":\"Use of Construction Spaces\""));
    }
    /**
     * Error scenario to test with invalid input URL.
     */
    @Test
    public void shouldReturnErrorWhenClassUriIsInvalid() {
        String classUri = "invaliuri";
        String dictionary = "bsdd";
        boolean withProperties = false;

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .queryParam("withProperties", withProperties)
                .body("\"" + classUri + "\"")
                .when()
                .post("/api/data-dictionary/{dictionary}/class", dictionary)
                .then()
                .contentType(ContentType.JSON)
                .log().all()
                .extract().response();

        String json = response.getBody().asString();
        assertTrue(json.contains("Internal Server Error"));
    }



    /**
     * Tests the property search API endpoint to ensure it returns matching
     * properties when provided with a valid dictionary and query string.
     */
    @Test
    public void shouldReturnMatchingPropertiesWhenQueryIsValid() {
        String dictionary = "bsdd";
        String query = "temperature";
        BsddMockStubHelper.stubListPropertiesApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/data-dictionary/{dictionary}/property/search/{query}", dictionary, query)
                .then()
                .statusCode(TestConstants.STATUS_SUCCESS)
                .contentType(ContentType.JSON)
                .log().all()
                .extract().response();

        response.then()
        .statusCode(HttpStatus.SC_SUCCESS)
        .contentType(ContentType.JSON)
        .body("[0].code", equalTo("ApplicationTemperature"))
        .body("[0].name", equalTo("ApplicationTemperature"))
        .body("[0].uri", equalTo("https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/ApplicationTemperature"))
        .body("[1].code", equalTo("ActivationTemperature"))
        .body("[1].name", equalTo("Activation Temperature"))
        .body("[1].uri", equalTo("https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/ActivationTemperature"));

    }

    /**
     * Tests the creation of a template including properties.
     */
    @Test
    public void shouldCreateTemplateWhenValidPropertiesUriAreGiven() {

        List<String> propertiesUriList = new ArrayList<>();
        propertiesUriList.add("https://identifier.buildingsmart.org/uri/etim/etim/10.0/prop/EF000008");
        String dictionary = "bsdd";

        BsddMockStubHelper.stubGetPropertiesApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .body(propertiesUriList)
                .when()
                .post("/api/data-dictionary/properties/{dictionary}", dictionary)
                .then()
                .statusCode(TestConstants.STATUS_SUCCESS)
                .contentType(ContentType.JSON)
                .log().all()
                .extract().response();

        response.then()
        .statusCode(TestConstants.STATUS_SUCCESS)
        .contentType(ContentType.JSON)
        .body("properties[0].code", equalTo("EF000008"))
        .body("properties[0].name", equalTo("Width"))
        .body("properties[0].uri", equalTo(
                "https://identifier.buildingsmart.org/uri/etim/etim/10.0/prop/EF000008"))
        .body("properties[0].dataType", equalTo("Real"))
        .body("properties[0].propertyValueKind", equalTo("Single"))
        .body("properties[0].status", equalTo("Active"));



    }
}
