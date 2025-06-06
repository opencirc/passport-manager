package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.oc.api.passport.PassportManager;
import com.oc.api.passport.config.AppProperties;
import com.oc.api.passport.constants.AppConstants;
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
public class TestTemplateController {

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
     * Tests the functionality of fetching data from the bsDD API.
     *
     * Verifies that the data retrieval works correctly and the expected response is
     * returned when the bsDD API is called with valid parameters.
     */
    @Test
    public void testFetchBsddData() throws JsonValidationException {
        String bsddUrl = "https://identifier.buildingsmart.org/uri/molio/cciconstruction"
                + "/1.0/class/A-A__";
        String ddLibrary = "bsdd";
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .queryParam("uri", bsddUrl).queryParam("ddLibrary", ddLibrary).when()
                .get("/api/template/class-with-props/").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType(ContentType.JSON)
                .log().all().extract().response();

        String json = response.getBody().asString();
        assertTrue(json.contains("\"name\":\"Use of Construction Spaces\""));
        assertTrue(json.contains("\"status\":\"Active\""));
        assertTrue(json.contains("\"dataCategory\":\"\""));
        assertTrue(json.contains("\"templateName\":\"\""));
    }

    /**
     * Tests the creation of a template inlcluding properties.
     */
    @Test
    public void testStub() {

        String queryParamUrl = "https://identifier.buildingsmart.org/uri/molio/"
                + "cciconstruction/1.0/class/A-A__";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(props.getBsDDClassDetailsURL())
                .queryParam(AppConstants.URI, queryParamUrl)
                .queryParam(AppConstants.QP_BSDD_INCLUDECLASSPROP, true);
        String url = uriBuilder.toUriString();
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(url,
                JsonNode.class);

        JsonNode jsonResponse = responseEntity.getBody();
        System.out.println(jsonResponse);
        assertNotNull(jsonResponse);
        assertEquals("A-A__", jsonResponse.get("referenceCode").asText());
        assertEquals("Space for human dwelling", jsonResponse.get("name").asText());
        assertEquals("Active", jsonResponse.get("status").asText());
    }


    /**
     * Tests the creation of a template including properties.
     */
    @Test
    public void testCreateTemplateWithProperties() {
        List<String> propertiesUriList = new ArrayList<String>();
        propertiesUriList.add("https://identifier.buildingsmart.org/uri/etim/etim/10.0/"
                + "prop/EF000008");
        String ddLibrary = "bsdd";
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .body(propertiesUriList)
                .queryParam("ddLibrary", ddLibrary).when()
                .post("/api/createTemplateWithProperties/").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType(ContentType.JSON)
                .log().all().extract().response();

        String json = response.getBody().asString();
        System.out.println(json);
    }
}
