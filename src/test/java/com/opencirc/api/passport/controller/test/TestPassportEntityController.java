package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

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
import com.oc.api.passport.exception.BsDDJsonValidationException;
import com.oc.api.passport.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.helper.test.BsddMockStubHelper;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestPassportEntityController {

    @LocalServerPort
    private int port;

    @MockBean
    private AuthUserDetailsService authUserDetailsService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppProperties props;
    
    private String jwtToken = null;
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("bsDD.classDetails.url",
                () -> "http://localhost:8089" + "/api/Class/v1");
        registry.add("bsDD.propertiesWithDetail.url",
                () -> "http://localhost:8089" + "/api/Property/v4");
    }

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
        if (response.getStatusCode() == 200) {
            jwtToken = response.getCookie("access_token");
        } else {
            throw new AssertionError(
                    "Expected status 200, but got " + response.getStatusCode());
        }

    }
    
    @Test
    public void testCreatePassportEntity_Success() throws BsDDJsonValidationException {
        String ddLibrary = "bsdd";
        String jsonBody = """
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
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody).queryParam("dictionaryName", ddLibrary).when()
                .post("/api/templateEntry").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType("application/text")
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.equalsIgnoreCase("Data saved successfully"));

    }
    
    @Test
    public void testCreatePassportEntity_Error_EmptyJsonBody() throws BsDDJsonValidationException {
        String ddLibrary = "bsdd";
        String jsonBody = """
                                {
                    
                 }
                                """;
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(jsonBody).queryParam("dictionaryName", ddLibrary).when()
                .post("/api/templateEntry").then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("Input JSON node is null"));

    }
    
    
    @Test
    public void testCreatePassportEntity_Error_InvalidJsonBody() throws BsDDJsonValidationException {
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
                .post("/api/templateEntry").then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().all().extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("Invalid Template"));

    }
}
