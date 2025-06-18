package com.opencirc.api.passport.helper.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import com.opencirc.api.passport.constants.test.TestConstants;


public class BsddMockStubHelper {

    /**
     * Stub to mock bsdd API response.
     */
    public static void stubGetClassApiResponse() {
        stubFor(get(urlPathEqualTo("/api/Class/v1"))
                .withQueryParam("Uri", equalTo("https://identifier.buildingsmart.org"
                        + "/uri/molio/cciconstruction/1.0/class/A-A__"))
                .withQueryParam("IncludeClassProperties", equalTo("true"))
                .willReturn(aResponse()
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
                        """
                    )));
        
    }
    
    
    public static void stubListPropertiesApiResponse() {
        stubFor(get(urlPathMatching("/bsdd/api/TextSearch/v2.*"))
            .withQueryParam("SearchText", matching(".*"))
            .withQueryParam("TypeFilter", equalTo("Property"))
            .withQueryParam("IncludeSearchDescriptions", equalTo("false"))
            .withQueryParam("Offset", equalTo("0"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(TestConstants.STATUS_SUCCESS)
                .withBody(
                    """
                    {
                      "classes": [],
                      "properties": [
                        {
                          "name": "Temperature Rating",
                          "uri": "https://identifier.buildingsmart.org/uri/etim/etim/8.0/prop/EF000008",
                          "code": "EF000008"
                        },
                        {
                          "name": "Operating Temperature",
                          "uri": "https://identifier.buildingsmart.org/uri/etim/etim/9.0/prop/EF000009",
                          "code": "EF000009"
                        }
                      ],
                      "totalCount": 2,
                      "offset": 0,
                      "count": 2
                    }
                    """
                )));
    }
    
    public static void stubGetPropertiesApiResponse() {
        stubFor(get(urlPathEqualTo("/api/Property/v4"))
                .withQueryParam("Uri", equalTo("https://identifier.buildingsmart.org/uri/etim/etim/10.0/prop/EF000008"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody("""
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
    public static void stubSearchClassApiResponse() {
        stubFor(get(urlPathMatching("/bsdd/api/Class/Search/v1.*"))
            .withQueryParam("SearchText", matching(".*"))
            .withQueryParam("limit", matching("20"))
            .willReturn(aResponse()
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
                    """
                )));
    }

    

    
}
