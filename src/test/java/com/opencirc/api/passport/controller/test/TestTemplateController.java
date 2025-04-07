package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.oc.api.passport.PassportManager;
import com.oc.api.passport.config.AppProperties;
import com.oc.api.passport.constants.AppConstants;
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
public class TestTemplateController {

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
    public void testFetchBsddData() throws BsDDJsonValidationException {
        String bsddUrl = "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__";
        String ddLibrary = "bsdd";
        BsddMockStubHelper.stubBsddApiResponse();

        Response response = RestAssured.given().log().all()
                .cookie("access_token", jwtToken).contentType(ContentType.JSON)
                .queryParam("uri", bsddUrl).queryParam("ddLibrary", ddLibrary).when()
                .get("/api/classes/template/").then()
                .statusCode(TestConstants.STATUS_SUCCESS).contentType(ContentType.JSON)
                .log().all().extract().response();

        String json = response.getBody().asString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
            System.out.println("Pretty Printed JSON: \n" + prettyJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(json.contains("\"referenceCode\":\"A-A__\""));
        assertTrue(json.contains("\"status\":\"Active\""));
    }

    @Test
    public void testStub() {

        BsddMockStubHelper.stubBsddApiResponse();

        String queryParamUrl = "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__";

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

}
