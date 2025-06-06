package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;

import com.oc.api.passport.PassportManager;
import com.oc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(classes = PassportManager.class,
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TestAuthController {

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
     * Configuration setup before test starts.
     */
    @BeforeEach
    public void setup() {
        RestAssured.port = port;
        MockitoAnnotations.openMocks(this);
        MockAuthenticationTestHelper helper = new MockAuthenticationTestHelper();
        helper.mockUserDetailsDB(authUserDetailsService, authenticationManager);

    }

    /**
     * This test ensures that sending credentials to the server are valid. If
     * so, returns non-null access and refresh tokens
     */

    @Test
    public void testLoginWithRightCredentials() {

        Response response = given().contentType(ContentType.JSON).body(
                "{\"username\": \"user1\", \"password\": \"user1password\"}")
                .when().post("/api/auth/login");
        response.then().statusCode(TestConstants.STATUS_SUCCESS)
        .contentType(ContentType.JSON);
        String accessToken = response.getCookie("access_token");
        assertNotNull(accessToken, "Access token should be present in the"
                + " response cookies");
        String refToken = response.getCookie("refresh_token");
        assertNotNull(refToken, "Access token should be present in the response cookies");

    }

    /**
     * This test ensures that sending credentials to the server are invalid.
     * Throws UnAuthorized Exception
     */
    @Test
    public void testLoginWithWrongCredentials() {

        Response response = given().contentType(ContentType.JSON).body(
                "{\"username\": \"user1d\", \"password\": \"wrongpassword\"}")
                .when().post("/api/auth/login");
        response.then().statusCode(TestConstants.STATUS_UNAUTHORIZED);
    }
}
