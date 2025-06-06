package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.oc.api.passport.PassportManager;
import com.oc.api.passport.config.AppProperties;
import com.oc.api.passport.exception.JsonValidationException;
import com.oc.api.passport.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.helper.test.BsddMockStubHelper;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestPassportEntityController {

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
    private RestTemplate restTemplate;

    /**
     * AppProperties bean.
     */
    @Autowired
    private AppProperties props;

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

        BsddMockStubHelper.stubBsddApiResponse();

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
     * Tests the successful creation of a passport entity with valid JSON input.
     */
    @Test
    public void testCreatePassportEntitySuccess() throws JsonValidationException {
        String ddLibrary = "bsdd";
        String jsonBody = """
                                {
                    "classType": "Class",
                    "referenceCode": "A-A__",
                    "relatedIfcEntityNames": [
                        "IfcSpace"
                    ],
                    "parentClassReference": {
                        "uri": "https://identifier.buildingsmart.org/uri/molio/
                        cciconstruction/1.0/class/uocs",
                        "name": "Use of Construction Spaces",
                        "code": "uocs"
                    },
                    "classProperties": [
                        {
                            "dataType": "Boolean",
                            "name": "Handicap Accessible",
                            "uri": "https://identifier.buildingsmart.org/uri/molio/
                            cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/
                            buildingsmart/ifc/4.3/prop/HandicapAccessible",
                            "actualValue": "true"
                        }
                    ],
                    "definition": "space designed for human dwelling
                    and related activities",
                    "name": "Space for human dwelling",
                    "uri": "https://identifier.buildingsmart.org/uri/molio/
                    cciconstruction/1.0/class/A-A__",
                    "status": "Active",
                    "templateName": "testTemplate",
                    "dataCategory": "Unique"
                }
                                """;
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody).queryParam("dictionaryName", ddLibrary).when()
                .post("/api/create-passport").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType("application/text")
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.equalsIgnoreCase("Data saved successfully"));

    }

    /**
     * Tests the behaviour of the createPassportEntity method when an empty
     * JSON body is provided.
     */
    @Test
    public void testCreatePassportEntityErrorEmptyJsonBody()
            throws JsonValidationException {
        String ddLibrary = "bsdd";
        String jsonBody = """
                                {

                 }
                                """;
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody).queryParam("dictionaryName", ddLibrary).when()
                .post("/api/create-passport").then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("Input JSON node is null"));

    }


    /**
     * Tests the behaviour of the createPassportEntity method when an invalid
     * JSON body is provided.
     */
    @Test
    public void testCreatePassportEntityErrorInvalidJsonBody()
            throws JsonValidationException {
        String ddLibrary = "bsdd";
        String jsonBody = """
                                {
                    "ABCDFWEREWRHIH":"ABCDFWEREWRHIH"
                 }
                                """;
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody).queryParam("dictionaryName", ddLibrary).when()
                .post("/api/create-passport").then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("Invalid Template"));

    }

    /**
     * Tests the successful update of a passport entity with valid input data.
     */
    @Test
    public void testUpdatePassportEntitySuccess() throws JsonValidationException {
        String passportEntityId = "b33ruul55gzk0idd0kxvggyofmtjfdg8u1ka";
        String jsonBody = """
                                {
                    "classType": "Class",
                    "referenceCode": "A-A__",
                    "relatedIfcEntityNames": [
                        "IfcSpace"
                    ],
                    "parentClassReference": {
                        "uri": "https://identifier.buildingsmart.org/uri/molio/
                        cciconstruction/1.0/class/uocs",
                        "name": "Use of Construction Spaces",
                        "code": "uocs"
                    },
                    "classProperties": [
                        {
                            "dataType": "String",
                            "name": "Handicap Accessible",
                            "uri": "https://identifier.buildingsmart.org/uri/molio/
                            cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/
                            buildingsmart/ifc/4.3/prop/HandicapAccessible",
                            "actualValue": "true"
                        }
                    ],
                    "definition": "space designed for human dwelling and
                    related activities",
                    "name": "Space for human dwelling",
                    "uri": "https://identifier.buildingsmart.org/uri/molio/
                    cciconstruction/1.0/class/A-A__",
                    "status": "Active",
                    "templateName": "testTemplate",
                    "dataCategory": "Unique"
                }
                                """;
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody)
                .queryParam("ddLibrary", "bsdd")
                .queryParam("passportEntityId", passportEntityId).when()
                .post("/api/passport/update/").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType("application/text")
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.equalsIgnoreCase("Successfully updated"));

    }

    /**
     * Tests the update operation when the specified passport entity is not found.
     */
    @Test
    public void testUpdatePassportEntityNotFound() {
        String invalidPassportId = "123543534534";
        String jsonBody = """
                                {
                    "classType": "Class",
                    "referenceCode": "A-A__",
                    "relatedIfcEntityNames": [
                        "IfcSpace"
                    ],
                    "parentClassReference": {
                        "uri": "https://identifier.buildingsmart.org/uri/molio/
                        cciconstruction/1.0/class/uocs",
                        "name": "Use of Construction Spaces",
                        "code": "uocs"
                    },
                    "classProperties": [
                        {
                            "dataType": "String",
                            "name": "Handicap Accessible",
                            "uri": "https://identifier.buildingsmart.org/uri/molio/
                            cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/
                            buildingsmart/ifc/4.3/prop/HandicapAccessible",
                            "actualValue": "true"
                        }
                    ],
                    "definition": "space designed for human dwelling and related
                    activities",
                    "name": "Space for human dwelling",
                    "uri": "https://identifier.buildingsmart.org/uri/
                    molio/cciconstruction/1.0/class/A-A__",
                    "status": "Active",
                    "templateName": "testTemplate",
                    "dataCategory": "Unique"
                }
                                """;

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody)
                .queryParam("ddLibrary", "bsdd")
                .queryParam("passportEntityId", invalidPassportId).when()
                .post("/api/passport/update/").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType("application/text")
                .log().all().extract().response();

        assertEquals("passport is not available to update",
                response.getBody().asString());
    }

    /**
     * Tests the update operation for a passport entity using invalid JSON input.
     */
    @Test
    public void testUpdatePassportEntityInvalidJson() {
        String invalidJson = "{ \"classType\": \"sdas\" }";
        String passportEntityId = "b33ruul55gzk0idd0kxvggyofmtjfdg8u1ka";

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(invalidJson)
                .queryParam("ddLibrary", "bsdd")
                .queryParam("passportEntityId", passportEntityId).when()
                .post("/api/passport/update/").then()
                .statusCode(400)
                .log().all().extract().response();
        System.out.println(response.getBody().asString());
        assertTrue(response.getBody().asString().contains("Invalid Template"));
    }
}
