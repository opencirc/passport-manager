package com.opencirc.api.passport.helper.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.opencirc.api.passport.constants.test.TestConstants;
import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BsddMockStubHelper {

  /** Stub to mock bsdd API response. */
  public static void stubGetClassApiResponse() {
    stubFor(
        get(urlPathEqualTo("/api/Class/v1"))
            .withQueryParam(
                "Uri",
                equalTo(
                    "https://identifier.buildingsmart.org"
                        + "/uri/molio/cciconstruction/1.0/class/A-A__"))
            .withQueryParam("IncludeClassProperties", equalTo("true"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(TestConstants.STATUS_SUCCESS)
                    .withBody(
                        """
                        {
                            "classType" : "Class",
                            "referenceCode" : "A-A__",
                            "relatedIfcEntityNames" : [ "IfcSpace" ],
                            "parentClassReference" : {
                                "uri" : "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/uocs",
                                "name" : "Use of Construction Spaces",
                                "code" : "uocs"
                            },
                            "classProperties" : [
                                {
                                    "name" : "Handicap Accessible",
                                    "uri" : "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
                                    "dataType" : "Boolean"
                                }
                            ],
                            "definition" : "space designed for human dwelling and related activities",
                            "name" : "Space for human dwelling",
                            "uri" : "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__",
                            "status" : "Active"
                        }
                        """)));
  }

  /** Creates mock stub data which returns list of properties for the matching text. */
  public static void stubListPropertiesApiResponse() {
    stubFor(
        get(urlPathMatching("/bsdd/api/TextSearch/v2.*"))
            .withQueryParam("SearchText", matching(".*"))
            .withQueryParam("TypeFilter", equalTo("Property"))
            .withQueryParam("IncludeSearchDescriptions", equalTo("false"))
            .withQueryParam("Offset", equalTo("0"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(TestConstants.STATUS_SUCCESS)
                    .withBody(
                        """
                    {
                      "classes": [],
                      "properties": [
                        {
                          "name": "ApplicationTemperature",
                          "uri": "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/ApplicationTemperature",
                          "code": "ApplicationTemperature"
                        },
                        {
                          "name": "Activation Temperature",
                          "uri": "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/prop/ActivationTemperature",
                          "code": "ActivationTemperature"
                        }
                      ],
                      "totalCount": 2,
                      "offset": 0,
                      "count": 2
                    }
                    """)));
  }

  /** Creates mock stub data which returns Properties details for the matching Uri. */
  public static void stubGetPropertiesApiResponse() {
    stubFor(
        get(urlPathEqualTo("/api/Property/v4"))
            .withQueryParam(
                "Uri",
                equalTo("https://identifier.buildingsmart.org/uri/etim/etim/10.0/prop/EF000008"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatus.SC_SUCCESS)
                    .withBody(
                        """
                        {
                          "connectedPropertyCodes": [],
                          "dataType": "Real",
                          "dimension": "1 0 0 0 0 0 0",
                          "dimensionLength": 1,
                          "dimensionMass": 0,
                          "dimensionTime": 0,
                          "dimensionElectricCurrent": 0,
                          "dimensionThermodynamicTemperature": 0,
                          "dimensionAmountOfSubstance": 0,
                          "dimensionLuminousIntensity": 0,
                          "example": "None",
                          "propertyValueKind": "Single",
                          "units": [
                            "m",
                            "cm",
                            "mm"
                          ],
                          "qudtCodes": [
                            "M",
                            "CentiM",
                            "MilliM"
                          ],
                          "dictionaryUri": "https://identifier.buildingsmart.org/uri/etim/etim/10.0",
                          "activationDateUtc": "2025-01-31T00:00:00Z",
                          "code": "EF000008",
                          "countriesOfUse": [],
                          "definition": "Overall dimension in the horizontal plane, not applicable for a round product",
                          "name": "Width",
                          "uri": "https://identifier.buildingsmart.org/uri/etim/etim/10.0/prop/EF000008",
                          "replacedObjectCodes": [],
                          "replacingObjectCodes": [],
                          "status": "Active",
                          "subdivisionsOfUse": [],
                          "uid": "None",
                          "versionDateUtc": "2024-12-04T00:00:00Z",
                          "visualRepresentationUri": "https://prod.etim-international.com/Feature"
                        }
                    """)));
  }

  /** Mock stub return class details for the matching text. */
  public static void stubSearchClassApiResponse() {
    stubFor(
        get(urlPathMatching("/bsdd/api/Class/Search/v1.*"))
            .withQueryParam("SearchText", matching(".*"))
            .withQueryParam("limit", matching("20"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(TestConstants.STATUS_SUCCESS)
                    .withBody(
                        """
                    {
                      "classes": [
                        {
      "dictionaryUri": "https://identifier.buildingsmart.org/uri/etim/etim/8.0",
      "dictionaryName": "ETIM",
      "name": "Storage container for hazardous material",
      "referenceCode": "EC004131",
      "uri": "https://identifier.buildingsmart.org/uri/etim/etim/8.0/class/EC004131",
      "classType": "Class",
      "parentClassName": "Accommodation/storage space",
      "relatedIfcEntityNames": []
    },
    {
      "dictionaryUri": "https://identifier.buildingsmart.org/uri/etim/etim/9.0",
      "dictionaryName": "ETIM",
      "name": "Storage container for hazardous material",
      "referenceCode": "EC004131",
      "uri": "https://identifier.buildingsmart.org/uri/etim/etim/9.0/class/EC004131",
      "classType": "Class",
      "parentClassName": "Accommodation/storage space",
      "relatedIfcEntityNames": []
    }
                      ],
                      "totalCount": 2,
                      "offset": 0,
                      "count": 2
                    }
                    """)));
  }

  /**
   * Class to mock the result set for Passport Datasheet queries.
   *
   * <p>This mock implementation is primarily used for testing the retrieval of passports and their
   * associated datasheets from a data source.
   */
  public static class TestPassportDatasheetResultMapDto implements PassportDatasheetResultMapDto {

    private final String passportId;

    private final String passportName;

    private final String status;

    private final String parentId;

    private final String passportCreatedById;

    private final String passportCreatedBy;

    private final LocalDateTime passportCreatedTime;

    private final String datasheetId;

    private final String platform;

    private final String dictionary;

    private final String datasheetCode;

    private final String datasheetName;

    private final String datasheetDescription;

    private final String datasheetPlatformId;

    private final String dataCategory;

    private final String data;

    private final String datasheetCreatedBy;

    private final String datasheetCreatedById;

    private final LocalDateTime datasheetCreatedTime;

    private final String datasheetPropertyId;

    private final String datasheetPropertyDatasheetId;

    private final String datasheetPropertyCode;

    private final String datasheetPropertyPlatformId;

    private final String datasheetPropertyGroupTag;

    private final String datasheetPropertyType;

    private final String datasheetPropertyDefinition;

    private TestPassportDatasheetResultMapDto(
        String passportId,
        String passportName,
        String status,
        String parentId,
        String passportCreatedById,
        String passportCreatedBy,
        LocalDateTime passportCreatedTime,
        String datasheetId,
        String platform,
        String dictionary,
        String datasheetCode,
        String datasheetName,
        String datasheetDescription,
        String datasheetPlatformId,
        String dataCategory,
        String data,
        String datasheetCreatedBy,
        String datasheetCreatedById,
        LocalDateTime datasheetCreatedTime,
        String datasheetPropertyId,
        String datasheetPropertyDatasheetId,
        String datasheetPropertyCode,
        String datasheetPropertyPlatformId,
        String datasheetPropertyGroupTag,
        String datasheetPropertyType,
        String datasheetPropertyDefinition) {
      super();
      this.passportId = passportId;
      this.passportName = passportName;
      this.status = status;
      this.parentId = parentId;
      this.passportCreatedById = passportCreatedById;
      this.passportCreatedBy = passportCreatedBy;
      this.passportCreatedTime = passportCreatedTime;
      this.datasheetId = datasheetId;
      this.platform = platform;
      this.dictionary = dictionary;
      this.datasheetCode = datasheetCode;
      this.datasheetName = datasheetName;
      this.datasheetDescription = datasheetDescription;
      this.datasheetPlatformId = datasheetPlatformId;
      this.dataCategory = dataCategory;
      this.data = data;
      this.datasheetCreatedBy = datasheetCreatedBy;
      this.datasheetCreatedById = datasheetCreatedById;
      this.datasheetCreatedTime = datasheetCreatedTime;
      this.datasheetPropertyId = datasheetPropertyId;
      this.datasheetPropertyDatasheetId = datasheetPropertyDatasheetId;
      this.datasheetPropertyCode = datasheetPropertyCode;
      this.datasheetPropertyPlatformId = datasheetPropertyPlatformId;
      this.datasheetPropertyGroupTag = datasheetPropertyGroupTag;
      this.datasheetPropertyType = datasheetPropertyType;
      this.datasheetPropertyDefinition = datasheetPropertyDefinition;
    }

    public String getPassportId() {
      return passportId;
    }

    public String getPassportName() {
      return passportName;
    }

    public String getStatus() {
      return status;
    }

    public String getParentId() {
      return parentId;
    }

    public String getPassportCreatedById() {
      return passportCreatedById;
    }

    public String getPassportCreatedBy() {
      return passportCreatedBy;
    }

    public String getDatasheetId() {
      return datasheetId;
    }

    public String getPlatform() {
      return platform;
    }

    public String getDictionary() {
      return dictionary;
    }

    public String getDatasheetCode() {
      return datasheetCode;
    }

    public String getDatasheetName() {
      return datasheetName;
    }

    public String getDatasheetDescription() {
      return datasheetDescription;
    }

    public String getDatasheetPlatformId() {
      return datasheetPlatformId;
    }

    public String getDataCategory() {
      return dataCategory;
    }

    public String getData() {
      return data;
    }

    public String getDatasheetCreatedBy() {
      return datasheetCreatedBy;
    }

    public String getDatasheetCreatedById() {
      return datasheetCreatedById;
    }

    public LocalDateTime getPassportCreatedTime() {
      return passportCreatedTime;
    }

    public String getDatasheetPropertyId() {
      return datasheetPropertyId;
    }

    public String getDatasheetPropertyDatasheetId() {
      return datasheetPropertyDatasheetId;
    }

    public String getDatasheetPropertyCode() {
      return datasheetPropertyCode;
    }

    public String getDatasheetPropertyPlatformId() {
      return datasheetPropertyPlatformId;
    }

    public String getDatasheetPropertyGroupTag() {
      return datasheetPropertyGroupTag;
    }

    public String getDatasheetPropertyType() {
      return datasheetPropertyType;
    }

    public String getDatasheetPropertyDefinition() {
      return datasheetPropertyDefinition;
    }

    public LocalDateTime getDatasheetCreatedTime() {
      return datasheetCreatedTime;
    }
  }

  /**
   * Creates mock stub data representing a parent passport with children. *
   *
   * @return List of {@link PassportDatasheetResultMapDto} representing mock passport data
   */
  public static List<PassportDatasheetResultMapDto> createPassportChildrenStubData() {
    String propertyDescriptionJson =
        """
          {
    "classType": "Class",
    "referenceCode": "\"\"",
    "relatedIfcEntityNames": [
        "IfcWasteTerminal"
    ],
    "parentClassReference": {
        "uri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/class/br18B2T6.5.2",
        "name": "Logistik",
        "code": "br18B2T6.5.2"
    },
    "classProperties": [
        {
            "name": "KlimapåvirkningBe18Tabel6",
            "uri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/class/br18B2T6.5.2.1/prop/Klimapåvirkning/br18P297stk4",
            "description": "BR 18, bilag 2, tabel 6 Bygningsdele til beregning af klimapåvirkning 10.01.2024,",
            "definition": "BR 18, bilag 2, tabel 6 Bygningsdele til beregning af klimapåvirkning 10.01.2024,",
            "dataType": "Boolean",
            "propertyCode": "br18P297stk4",
            "propertyDictionaryName": "Tabel6",
            "dictionaryUri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1",
            "propertyUri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/prop/br18P297stk4",
            "propertySet": "Klimapåvirkning",
            "status": "Active",
            "propertyValueKind": "Single",
            "actualValue": "true"
        }
    ],
    "dictionaryUri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1",
    "activationDateUtc": "9999-12-31T23:59:59Z",
    "code": "br18B2T6.5.2.1",
    "definition": "Affald er en del af Logistik som kategoriseres under: VVS-anlæg",
    "name": "Affald",
    "uri": "https://identifier.buildingsmart.org/uri/dtu.construct/br18B2T6/1.1.1/class/br18B2T6.5.2.1",
    "status": "Preview",
    "subdivisionsOfUse": [],
    "versionDateUtc": "2025-11-13T00:00:00Z"
}
          """;

    String datasheetJson = "{\n" + "  \"br18P297stk4\": true,\n" + "}";

    LocalDateTime timestamp = LocalDateTime.of(2025, 6, 10, 12, 0);

    List<PassportDatasheetResultMapDto> stubData = new ArrayList<>();

    // ---------------- Parent Passport ----------------
    stubData.add(
        new TestPassportDatasheetResultMapDto(
            "w1yi7790bs0mutg7i8kumbv9t6pdrf83wqan",
            "Parent Passport1",
            "active",
            null,
            "87510a3c-4357-47bc-80a1-9ed02285fbae",
            "admin@test.com",
            timestamp,
            "fb01f7dc-2294-4cd1-882c-ecae17c41ffa", // datasheetId
            "bsdd", // platform
            "ifc", // dictionary
            "HandicapAccessible", // datasheetCode
            "HandicapAccessible", // datasheetName
            "HandicapAccessible", // datasheetDescription
            "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible", // datasheetPlatformId
            "generic", // dataCategory
            datasheetJson, // data
            "{\"email\": \"admin@test.com\",  \"fullName\": \"test admin\"}", // datasheetCreatedBy
            "717753dc-ba6c-4a8d-87c9-cce878986553", // datasheetCreatedById
            timestamp, // datasheetCreatedTime
            "26c8cb81-3468-4c12-9ea0-b16b45b4712b", // datasheetPropertyId
            "fb01f7dc-2294-4cd1-882c-ecae17c41ffa", // datasheetPropertyDatasheetId
            "EF001391", // datasheetPropertyCode
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0", // datasheetPropertyPlatformId
            "Electrical", // datasheetPropertyGroupTag
            "Boolean", // datasheetPropertyType
            propertyDescriptionJson // datasheetPropertyDefinition
            ));

    // ---------------- Child 1 ----------------
    stubData.add(
        new TestPassportDatasheetResultMapDto(
            "oqj4p875porh0vpuqj1vob2jqgymchild001",
            "Child Passport 1",
            "active",
            "w1yi7790bs0mutg7i8kumbv9t6pdrf83wqan",
            "87510a3c-4357-47bc-80a1-9ed02285fbae",
            "admin@test.com",
            timestamp,
            "fb01f7dc-2294-4cd1-882c-ecae17c41ffa",
            "bsdd",
            "ifc",
            "HandicapAccessible",
            "HandicapAccessible",
            "HandicapAccessible",
            "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
            "generic",
            datasheetJson,
            "{\"email\": \"admin@test.com\",  \"fullName\": \"test admin\"}",
            "717753dc-ba6c-4a8d-87c9-cce878986553",
            timestamp,
            "26c8cb81-3468-4c12-9ea0-b16b45b4712b",
            "fb01f7dc-2294-4cd1-882c-ecae17c41ffa",
            "EF001391", // datasheetPropertyCode
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0",
            "Electrical",
            "Boolean",
            propertyDescriptionJson));

    // ---------------- Child 2 ----------------
    stubData.add(
        new TestPassportDatasheetResultMapDto(
            "oqj4p875porh0vpuqj1vob2jqgymchild002",
            "Child Passport 2",
            "active",
            "w1yi7790bs0mutg7i8kumbv9t6pdrf83wqan",
            "87510a3c-4357-47bc-80a1-9ed02285fbae",
            "admin@test.com",
            timestamp,
            "fb01f7dc-2294-4cd1-882c-ecae17c41ffa",
            "bsdd",
            "ifc",
            "HandicapAccessible",
            "HandicapAccessible",
            "HandicapAccessible",
            "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
            "generic",
            datasheetJson,
            "{\"email\": \"admin@test.com\",  \"fullName\": \"test admin\"}",
            "717753dc-ba6c-4a8d-87c9-cce878986553",
            timestamp,
            "26c8cb81-3468-4c12-9ea0-b16b45b4712b",
            "fb01f7dc-2294-4cd1-882c-ecae17c41ffa",
            "EF001391", // datasheetPropertyCode
            "https://identifier.buildingsmart.org/uri/etim/etim/10.0",
            "Electrical",
            "Boolean",
            propertyDescriptionJson));

    return stubData;
  }
}
