package com.opencirc.api.passport.controller;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.opencirc.api.passport.helper.MockAuthenticationHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@SpringBootTest(classes = PassportManager.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestTemplateController {

    @Mock
    private TemplateService templateService;
    
    
    @Mock
    private BsDDAdapter bsDDAdapter;
    
    @Mock
    private DictionaryAdapterFactory dictionaryAdapterFactory;

    @InjectMocks
    private TemplateController templateController;

    private String jwtToken;
    
    @LocalServerPort
    private int port;
    
	
	@MockBean 
	private AuthUserDetailsService authUserDetailsService;

	@MockBean
	private AuthenticationManager authenticationManager;
    
    @BeforeEach
    public void setUp() {
    	RestAssured.port = port;
    	MockAuthenticationHelper helper = new MockAuthenticationHelper();
		helper.mockUserDetailsDB(authUserDetailsService, authenticationManager);
        mockJwtTokenGeneration();
      
    }

    private void mockJwtTokenGeneration() {
        // Mocking JWT token generation process, you can create a valid JWT here
       // jwtToken = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTc0MTgzMDQ1MCwiZXhwIjoxNzQxOTE2ODUwfQ.Q0aCTNzhKQe9FkIpuTxnULXP8FvXyfY-9-ESNKtkN3A"; // replace with your real token generation logic
    	String requestBody = "{\"username\": \"user1\", \"password\": \"user1password\"}";

        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract().response();

        jwtToken = response.jsonPath().getString("accessToken");
        
        System.out.println(" jwtToken "+jwtToken);
    }

    
    @Test
    public void testListClassesByText() {
        String searchText = "iso"; 
        String ddLibrary = "bsdd"; 

        
        Response response =  given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            
        .when()
            .get("/api/classes/search/{searchText}/{ddLibrary}", searchText, ddLibrary);
        System.out.println("Response Body: " + response.getBody().asString());
        response.then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(greaterThan(0))) 
            .body("[0]", hasKey("code")) 
            .body("[0]", hasKey("name")); 
    }
}

