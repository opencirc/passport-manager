package com.opencirc.api.passport.controller.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.opencirc.api.passport.PassportManager;
import com.opencirc.api.passport.auth.service.AuthUserDetailsService;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportRequestDto;
import com.opencirc.api.passport.dto.PassportDatasheetResultMapDto;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.helper.test.BsddMockStubHelper;
import com.opencirc.api.passport.helper.test.MockAuthenticationTestHelper;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.service.PassportService;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.servlet.http.Cookie;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = PassportManager.class)
@WireMockTest(httpPort = 8089)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TestPassportController {

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

    
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PassportRepository passportRepository; 
    
    @MockBean
    private DatasheetRepository datasheetRepository; 
    
    @MockBean
    private PassportDatasheetMappingRepository passportDatasheetMappingRepository;
    
    @SpyBean
    private PassportService passportService;

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
     * Tests the successful creation of a passport with valid JSON input.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testCreatePassportSuccess() throws JsonValidationException, JsonMappingException, JsonProcessingException {
        String dictionary = "bsdd";
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
        ObjectMapper objectMapper = new ObjectMapper();
        CreatePassportRequestDto createPassportRequest = new CreatePassportRequestDto();
        createPassportRequest.setCreatedTime(LocalDateTime.now());
        createPassportRequest.setDataCategory(DataCategory.GENERIC.getValue());
        createPassportRequest.setDatasheetData(objectMapper.readTree(jsonBody));
        createPassportRequest.setPassportName("Dwelling Space");
        createPassportRequest.setCreatedBy("Test");

        Passport mockPassport = new Passport();
        mockPassport.setId("mock-passport-id");
        mockPassport.setName("Dwelling Space");
        mockPassport.setCreatedBy("Test");
        mockPassport.setCreatedTime(LocalDateTime.now());
        mockPassport.setStatus(Passport.Status.ACTIVE);

        Datasheet mockDatasheet = new Datasheet();
        mockDatasheet.setId(1);
        mockDatasheet.setCreatedBy("Test");
        mockDatasheet.setCreatedTime(LocalDateTime.now());

        PassportDatasheetMapping mockMapping = new PassportDatasheetMapping();
        mockMapping.setId(123L);
        mockMapping.setPassport(mockPassport);
        mockMapping.setDatasheet(mockDatasheet);

        Mockito.when(passportRepository.save(Mockito.any(Passport.class)))
               .thenReturn(mockPassport);

        Mockito.when(datasheetRepository.save(Mockito.any(Datasheet.class)))
               .thenReturn(mockDatasheet);

        Mockito.when(passportDatasheetMappingRepository.save(Mockito.any(PassportDatasheetMapping.class)))
               .thenReturn(mockMapping);

        // Call the API
        Response response = RestAssured.given()
                .log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .body(createPassportRequest)
                .pathParam("dictionary", dictionary)
                .when()
                .post("/api/passport-entity/dictionary/{dictionary}/")
                .then()
                .statusCode(HttpStatus.SC_SUCCESS)
                .contentType(ContentType.JSON)
                .log().all()
                .extract().response();

        // Assertions
        response.then()
            .body("name", equalTo("Dwelling Space"));
    }

    /**
     * Tests the behaviour of the createPassport method when an empty
     * JSON body is provided.
     */
    @Test
    public void testCreatePassportErrorEmptyJsonBody()
            throws JsonValidationException {
        String dictionary = "bsdd";
        CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
        requestDto.setCreatedTime(LocalDateTime.now());
        requestDto.setCreatedBy("Test");
        requestDto.setPassportName("Empty Passport");

        Response response = RestAssured.given()
                .log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .body(requestDto)
                .pathParam("dictionary", dictionary)
                .when()
                .post("/api/passport-entity/dictionary/{dictionary}/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().all()
                .extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("Input JSON node is null"));

    }


    /**
     * Tests the behaviour of the createPassport method when an invalid
     * JSON body is provided.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Test
    public void testCreatePassportErrorInvalidJsonBody()
            throws JsonValidationException, JsonMappingException, JsonProcessingException {
        String dictionary = "bsdd";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode invalidNode = objectMapper.readTree("""
            {
                "ABCDFWEREWRHIH": "ABCDFWEREWRHIH"
            }
        """);
        
        CreatePassportRequestDto requestDto = new CreatePassportRequestDto();
        requestDto.setCreatedTime(LocalDateTime.now());
        requestDto.setCreatedBy("Test");
        requestDto.setPassportName("Invalid Passport");
        requestDto.setDataCategory(DataCategory.GENERIC.getValue());
        requestDto.setDatasheetData(invalidNode);
        

        Response response = RestAssured.given()
                .log().all()
                .cookie("access_token", jwtToken)
                .contentType(ContentType.JSON)
                .body(requestDto)
                .pathParam("dictionary", dictionary)
                .when()
                .post("/api/passport-entity/dictionary/{dictionary}/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().all()
                .extract().response();

        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("Invalid Template"));

    }
    
    
    @Test
    public void testGetPassportSuccess() throws Exception {

        String passportId = "oqj4p875porh0vpuqj1vob2jqgym4b706oe9";

        Passport passport = new Passport();
        passport.setId(passportId);
        passport.setStatus(Passport.Status.ACTIVE);
        passport.setName("Dwelling Space");
        PassportDatasheetMapping dataSheetMapping = new PassportDatasheetMapping();
        Datasheet datasheet = new Datasheet();
        datasheet.setData(getDataSheetData());
        dataSheetMapping.setDatasheet(datasheet);
        dataSheetMapping.setPassport(passport);
        dataSheetMapping.setId(1l);
        passport.setDatasheetMappings(List.of(dataSheetMapping));

        Mockito.when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
                .thenReturn(Optional.of(passport));

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/passport/{id}/", passportId)
                        .cookie(new Cookie("access_token", jwtToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(passportId))
                .andExpect(jsonPath("$.name").value("Dwelling Space")).andReturn(); 

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Response JSON:\n" + responseBody);

    }

    
    @Test
    public void testGetPassportChildrenSuccess() throws Exception {

        String passportId = "oqj4p875porh0vpuqj1vob2jqgymchildren";
        
        List<PassportDatasheetResultMapDto> stubData = BsddMockStubHelper.createPassportChildrenStubData();
        Mockito.when(passportRepository.findActivePassportDescendants(passportId))
               .thenReturn(Optional.of(stubData));
        
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/passport/{id}/children", passportId)
                        .cookie(new Cookie("access_token", jwtToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(passportId))
                .andExpect(jsonPath("$[1].parent.id").value(passportId))
                .andReturn(); 

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Response JSON:\n" + responseBody);

    }
    
    
    private JsonNode getDataSheetData() throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = """
                {
            "id": 7,
            "data": {
                "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__",
                "code": "A-A__",
                "name": "Space for human dwelling",
                "status": "Active",
                "classType": "Class",
                "classProperties": [
                    {
                        "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
                        "code": "HandicapAccessible",
                        "name": "Handicap Accessible",
                        "status": "Active",
                        "dataType": "Boolean",
                        "definition": "Indication that this object is designed to be accessible by the handicapped.",
                        "actualValue": ""
                    }
                ]
            },
            "dataCategory": null,
            "dataDictionary": null,
            "createdBy": "abc@example.com",
            "createdTime": "2025-06-10T12:00:00"
        }
                """;

        return objectMapper.readTree(jsonBody);

    }
}
