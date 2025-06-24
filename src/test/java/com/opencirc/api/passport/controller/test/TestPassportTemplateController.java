package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TestPassportTemplateController {

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
     * Tests successful creation of a passport template from an existing passport.
     *
     */
    @Test
    public void shouldGenerateTemplateFromPassportSuccessfullyWithoutSaving() throws Exception {
        String templateName = "testtemplate";
        boolean dryRun = true;

        String passportId = "oqj4p875porh0vpuqj1vob2jqgym4b706oe9";

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .queryParam("dryRun", dryRun).pathParam("passportId", passportId)
                .body(templateName).when().post("/api/passport-template/{passportId}/")
                .then().statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON).log().all()
                .extract().response();

        response.then().body("template.name", equalTo("Space for human dwelling"))
                .body("template.classProperties[0].actualValue", equalTo(""));

    }

    /**
     * Tests failure case when trying to create a template from a invalid
     * passport.
     * Verifies that the API returns HTTP 404 when an invalid passport ID is passed.
     */
    @Test
    public void shouldReturnNotFoundWhenPassportNotFound() {
        String passportId = "invalid-id";
        boolean dryRun = true;
        String templateName = "My Template";

        Response response = RestAssured.given().cookie("access_token", jwtToken)
                .contentType(ContentType.JSON).queryParam("dryRun", dryRun)
                .body(templateName)
                .pathParam("passportId", passportId).when()
                .post("/api/passport-template/{passportId}/")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().response();

        assertTrue(response.getBody().asString().contains("Active passport not found"));
    }

    /**
     * Tests successful retrieval of a persisted passport template.
     * Ensures that the template is returned correctly for a valid template ID.
     */
    @Test
    public void shouldRetrieveTemplateSuccessfullyById() {
        String templateId = "f906156c-4602-4366-bc01-2790df93d803";

        Response response = RestAssured.given()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .pathParam("id", templateId)
                .when()
                .get("/api/passport-template/{id}/")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .log().all().extract().response();

        assertTrue(response.getBody().asString().contains(templateId));

        response.then().body("id", equalTo(templateId))
                .body("template.name", equalTo("Space for human dwelling"))
                .body("template.classProperties[0].actualValue", equalTo(""));
    }

    /**
     * Tests failure to fetch a template that does not exist.
     * Ensures that HTTP 404.
     */
    @Test
    public void shouldReturnNotFoundForInvalidTemplateId() {
        String templateId = "00000000-0000-0000-0000-00000000000";

        Response response = RestAssured.given()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .pathParam("id", templateId).when()
                .get("/api/passport-template/{id}/")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .log().all().extract().response();

        assertTrue(response.getBody().asString().contains(
                "Passport template not found for ID: "
                + "00000000-0000-0000-0000-00000000000"));
    }


    /**
     * Tests retrieval of all persisted passport templates.
     * Ensures that a list of templates is returned and is not empty.
     */
    @Test
    public void shouldListAllPersistedTemplates() {
        Response response = RestAssured.given()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/passport-templates/all")
                .then().statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .log().all().extract().response();

        List<Map<String, Object>> templates = response.jsonPath().getList("$");
        assertNotNull(templates);
        assertFalse(templates.isEmpty());
    }
}
