package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.AuthenticationManager;

import com.oc.api.passport.PassportManager;
import com.oc.api.passport.adapter.BsDDAdapter;
import com.oc.api.passport.adapter.DictionaryAdapterFactory;
import com.oc.api.passport.controller.TemplateController;
import com.oc.api.passport.service.AuthUserDetailsService;
import com.oc.api.passport.service.TemplateService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(classes = PassportManager.class,
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestTemplateController {

    /**
     * TemplateService mock bean.
     */
    @Mock
    private TemplateService templateService;

    /**
     * BsDDAdapter mock bean.
     */
    @Mock
    private BsDDAdapter bsDDAdapter;

    /**
     * DictionaryAdapterFactory mock bean.
     */
    @Mock
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    /**
     * TemplateController mock bean.
     */
    @InjectMocks
    private TemplateController templateController;

    /**
     * Jwt token.
     */
    private String jwtToken;

    /**
     * The port number for url.
     */
    @LocalServerPort
    private int port;

    /**
     * AuthUserDetailsService mock bean.
     */
    @MockBean
    private AuthUserDetailsService authUserDetailsService;

    /**
     * AuthenticationManager mock bean.
     */
    @MockBean
    private AuthenticationManager authenticationManager;

    /**
     * Configuration setup before each test starts.
     */
    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        MockAuthenticationTestHelper helper = new MockAuthenticationTestHelper();
        helper.mockUserDetailsDB(authUserDetailsService, authenticationManager);
        mockJwtTokenGeneration();

    }

    /**
     * This method generates mock JWT access token to hit the api.
     */
    private void mockJwtTokenGeneration() {
        String requestBody = "{\"username\": \"user1\", \"password\": \"user1password\"}";

        Response response = given().contentType(ContentType.JSON)
                .body(requestBody).when().post("/api/auth/login").then()
                .statusCode(TestConstants.STATUS_SUCCESS)
                .body("accessToken", notNullValue()).extract()
                .response();

        jwtToken = response.jsonPath().getString("accessToken");
    }

    /**
     * This test returns response from the data dictionary library for the given
     * search text.
     */
    @Test
    public void testListClassesByText() {
        String searchText = "iso";
        String ddLibrary = "bsdd";

        Response response = given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)

                .when().get("/api/classes/search/{searchText}/{ddLibrary}",
                        searchText, ddLibrary);
        response.then().statusCode(TestConstants.STATUS_SUCCESS)
        .contentType(ContentType.JSON)
                .body("$", hasSize(greaterThan(0))).body("[0]", hasKey("code"))
                .body("[0]", hasKey("name"));
    }
}
