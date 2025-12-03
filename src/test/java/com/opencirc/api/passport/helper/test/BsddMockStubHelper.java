package com.opencirc.api.passport.helper.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

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
                    .withStatus(HttpStatus.OK.value())
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
                    .withStatus(HttpStatus.OK.value())
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
                    .withStatus(HttpStatus.OK.value())
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
                    .withStatus(HttpStatus.OK.value())
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
  public static class TestPassportDatasheetResultMapQueryResult
      implements PassportDatasheetResultMapQueryResult {
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

    private final String datasheetCreatedById;

    private final String datasheetCreatedBy;

    private final LocalDateTime datasheetCreatedTime;

    private final String datasheetPropertyId;

    private final String datasheetPropertyDatasheetId;

    private final String datasheetPropertyCode;

    private final String datasheetPropertyPlatformId;

    private final String datasheetPropertyGroupTag;

    private final String datasheetPropertyType;

    private final String datasheetPropertyDefinition;

    /** Constructor to initialize all fields of the mock DTO. */
    public TestPassportDatasheetResultMapQueryResult(
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
        String datasheetCreatedById,
        String datasheetCreatedBy,
        LocalDateTime datasheetCreatedTime,
        String datasheetPropertyId,
        String datasheetPropertyDatasheetId,
        String datasheetPropertyCode,
        String datasheetPropertyPlatformId,
        String datasheetPropertyGroupTag,
        String datasheetPropertyType,
        String datasheetPropertyDefinition) {
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
      this.datasheetCreatedById = datasheetCreatedById;
      this.datasheetCreatedBy = datasheetCreatedBy;
      this.datasheetCreatedTime = datasheetCreatedTime;
      this.datasheetPropertyId = datasheetPropertyId;
      this.datasheetPropertyDatasheetId = datasheetPropertyDatasheetId;
      this.datasheetPropertyCode = datasheetPropertyCode;
      this.datasheetPropertyPlatformId = datasheetPropertyPlatformId;
      this.datasheetPropertyGroupTag = datasheetPropertyGroupTag;
      this.datasheetPropertyType = datasheetPropertyType;
      this.datasheetPropertyDefinition = datasheetPropertyDefinition;
    }

    /** Returns the ID of the passport. */
    public String getPassportId() {
      return this.passportId;
    }

    /** Returns the name of the passport. */
    public String getPassportName() {
      return this.passportName;
    }

    /** Returns the status of the passport. */
    public String getStatus() {
      return this.status;
    }

    /** Returns the parent ID of the passport (if any). */
    public String getParentId() {
      return this.parentId;
    }

    /** Returns the user id who created the passport. */
    public String getPassportCreatedById() {
      return this.passportCreatedById;
    }

    /** Returns the JSON (as text) containing user information who created the passport. */
    public String getPassportCreatedBy() {
      return this.passportCreatedBy;
    }

    /** Returns the creation timestamp of the passport. */
    public LocalDateTime getPassportCreatedTime() {
      return this.passportCreatedTime;
    }

    /** Returns the ID of the associated datasheet. */
    public String getDatasheetId() {
      return this.datasheetId;
    }

    /** Returns the platform. */
    public String getPlatform() {
      return this.platform;
    }

    /** Returns the dictionary. */
    public String getDictionary() {
      return this.dictionary;
    }

    /** Returns the code. */
    public String getDatasheetCode() {
      return this.datasheetCode;
    }

    /** Returns the name. */
    public String getDatasheetName() {
      return this.datasheetName;
    }

    /** Returns the description. */
    public String getDatasheetDescription() {
      return this.datasheetDescription;
    }

    /** Returns the platform id. */
    public String getDatasheetPlatformId() {
      return this.datasheetPlatformId;
    }

    /** Returns the category of the datasheet (e.g., UNIQUE or GENERIC). */
    public String getDataCategory() {
      return this.dataCategory;
    }

    /** Returns the JSON data of the datasheet as a string. */
    public String getData() {
      return this.data;
    }

    /** Returns the user id who created the datasheet. */
    public String getDatasheetCreatedById() {
      return this.datasheetCreatedById;
    }

    /** Returns the JSON (as text) containing user information who created the datasheet. */
    public String getDatasheetCreatedBy() {
      return this.datasheetCreatedBy;
    }

    /** Returns the creation timestamp of the datasheet. */
    public LocalDateTime getDatasheetCreatedTime() {
      return this.datasheetCreatedTime;
    }

    /** Returns the ID of the associated datasheet property. */
    public String getDatasheetPropertyId() {
      return this.datasheetPropertyId;
    }

    /** Returns the ID of the associated datasheet. */
    public String getDatasheetPropertyDatasheetId() {
      return this.datasheetPropertyDatasheetId;
    }

    /** Returns the property code. */
    public String getDatasheetPropertyCode() {
      return this.datasheetPropertyCode;
    }

    /** Returns the ID of the platform. */
    public String getDatasheetPropertyPlatformId() {
      return this.datasheetPropertyPlatformId;
    }

    /** Returns the group where the property belongs to. */
    public String getDatasheetPropertyGroupTag() {
      return this.datasheetPropertyGroupTag;
    }

    /** Returns the type. */
    public String getDatasheetPropertyType() {
      return this.datasheetPropertyType;
    }

    /** Returns the definition of datasheet property. */
    public String getDatasheetPropertyDefinition() {
      return this.datasheetPropertyDefinition;
    }
  }

  /** Creates mock stub data representing a parent passport with children. * */
  public static List<PassportDatasheetResultMapQueryResult> createPassportChildrenStubData() {
    String datasheetJson =
        """
            {
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
            }
            """;

    Timestamp timestamp = Timestamp.valueOf("2025-06-10 12:00:00");

    List<PassportDatasheetResultMapQueryResult> stubData = new ArrayList<>();

    stubData.add(
        new TestPassportDatasheetResultMapQueryResult(
            "oqj4p875porh0vpuqj1vob2jqgymchildren",
            "Parent Passport",
            "active",
            null,
            "1",
            datasheetJson,
            null,
            null,
            "parent@example.com",
            timestamp.toString(),
            "A-A__",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null));

    stubData.add(
        new TestPassportDatasheetResultMapQueryResult(
            "oqj4p875porh0vpuqj1vob2jqgymchild001",
            "Child Passport 1",
            "active",
            "oqj4p875porh0vpuqj1vob2jqgymchildren",
            "2",
            datasheetJson,
            null,
            null,
            "child1@example.com",
            timestamp.toString(),
            "A-A__",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null));

    stubData.add(
        new TestPassportDatasheetResultMapQueryResult(
            "oqj4p875porh0vpuqj1vob2jqgymchild002",
            "Child Passport 2",
            "active",
            "oqj4p875porh0vpuqj1vob2jqgymchildren",
            "3",
            datasheetJson,
            null,
            null,
            "child2@example.com",
            timestamp.toString(),
            "A-A__",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null));

    return stubData;
  }
}
