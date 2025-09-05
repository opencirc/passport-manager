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
import com.opencirc.api.passport.dto.PassportDatasheetResultMapDto;
import java.sql.Timestamp;
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

    /** Unique identifier for the passport. */
    private final String passportId;

    /** Name of the passport. */
    private final String passportName;

    /** Current status of the passport. */
    private final String status;

    /** Identifier of the parent passport, if this passport is a child; may be null. */
    private final String parentId;

    /** Unique identifier for the associated datasheet. */
    private final String datasheetId;

    /** JSON string containing the datasheet information. */
    private final String data;

    /** URI of the data dictionary. */
    private final String dataDictionary;

    /** Category of the data. */
    private final String dataCategory;

    /** Created By. */
    private final String createdBy;

    /** Time of the creation. */
    private final Timestamp createdTime;

    /**
     * Constructor to initialize all fields of the mock DTO.
     *
     * @param passportId
     * @param passportName
     * @param status
     * @param parentId
     * @param datasheetId
     * @param data
     * @param dataDictionary
     * @param dataCategory
     * @param createdBy
     * @param createdTime
     */
    public TestPassportDatasheetResultMapDto(
        String passportId,
        String passportName,
        String status,
        String parentId,
        String datasheetId,
        String data,
        String dataDictionary,
        String dataCategory,
        String createdBy,
        Timestamp createdTime) {
      this.passportId = passportId;
      this.passportName = passportName;
      this.status = status;
      this.parentId = parentId;
      this.datasheetId = datasheetId;
      this.data = data;
      this.dataDictionary = dataDictionary;
      this.dataCategory = dataCategory;
      this.createdBy = createdBy;
      this.createdTime = createdTime;
    }

    /**
     * @return Passport ID
     */
    public String getPassportId() {
      return passportId;
    }

    /**
     * @return Passport name
     */
    public String getPassportName() {
      return passportName;
    }

    /**
     * @return Passport status
     */
    public String getStatus() {
      return status;
    }

    /**
     * @return Parent passport ID
     */
    public String getParentId() {
      return parentId;
    }

    /**
     * @return Datasheet ID
     */
    public String getDatasheetId() {
      return datasheetId;
    }

    /**
     * @return Datasheet JSON data
     */
    public String getData() {
      return data;
    }

    /**
     * @return URI of the data dictionary
     */
    public String getDataDictionary() {
      return dataDictionary;
    }

    /**
     * @return Data category
     */
    public String getDataCategory() {
      return dataCategory;
    }

    /**
     * @return User who created the record
     */
    public String getCreatedBy() {
      return createdBy;
    }

    /**
     * @return Timestamp of creation
     */
    public Timestamp getCreatedTime() {
      return createdTime;
    }
  }

  /**
   * Creates mock stub data representing a parent passport with children. *
   *
   * @return List of {@link PassportDatasheetResultMapDto} representing mock passport data
   */
  public static List<PassportDatasheetResultMapDto> createPassportChildrenStubData() {
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

    List<PassportDatasheetResultMapDto> stubData = new ArrayList<>();

    stubData.add(
        new TestPassportDatasheetResultMapDto(
            "oqj4p875porh0vpuqj1vob2jqgymchildren",
            "Parent Passport",
            "active",
            null,
            "1",
            datasheetJson,
            null,
            null,
            "parent@example.com",
            timestamp));

    stubData.add(
        new TestPassportDatasheetResultMapDto(
            "oqj4p875porh0vpuqj1vob2jqgymchild001",
            "Child Passport 1",
            "active",
            "oqj4p875porh0vpuqj1vob2jqgymchildren",
            "2",
            datasheetJson,
            null,
            null,
            "child1@example.com",
            timestamp));

    stubData.add(
        new TestPassportDatasheetResultMapDto(
            "oqj4p875porh0vpuqj1vob2jqgymchild002",
            "Child Passport 2",
            "active",
            "oqj4p875porh0vpuqj1vob2jqgymchildren",
            "3",
            datasheetJson,
            null,
            null,
            "child2@example.com",
            timestamp));

    return stubData;
  }
}
