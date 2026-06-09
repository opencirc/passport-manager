package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.service.PassportService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {PassportManager.class, TestConfig.class})
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
public class TestPassportController {

  @LocalServerPort private int port;

  @MockBean private AuthUserDetailsService authUserDetailsService;

  @MockBean private AuthenticationManager authenticationManager;

  @MockBean private PassportService passportService;

  private String jwtToken = null;

  @BeforeEach
  void setPortsAndMocks(WireMockRuntimeInfo wmInfo) {
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
      throw new AssertionError("Expected status 200, but got " + response.getStatusCode());
    }
  }

  @Test
  public void getImmediateChildren_ShouldReturnOk_WhenPassportExists() {
    String passportId = "test-id";
    PassportDto child = new PassportDto();
    child.setId("child-id");
    child.setName("Child Name");

    when(passportService.getImmediateChildren(passportId)).thenReturn(List.of(child));

    given()
        .cookie("access_token", jwtToken)
        .pathParam("passportId", passportId)
        .when()
        .get("/api/passport/{passportId}/immediateChildren")
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("child-id"))
        .body("[0].name", equalTo("Child Name"));
  }

  @Test
  public void getImmediateChildren_ShouldReturnNotFound_WhenPassportDoesNotExist() {
    String passportId = "non-existent-id";

    when(passportService.getImmediateChildren(passportId))
        .thenThrow(new HttpServerErrorException(HttpStatus.NOT_FOUND, "Not found"));

    given()
        .cookie("access_token", jwtToken)
        .pathParam("passportId", passportId)
        .when()
        .get("/api/passport/{passportId}/immediateChildren")
        .then()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }
}
