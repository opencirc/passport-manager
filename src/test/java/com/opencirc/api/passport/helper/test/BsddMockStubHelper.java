package com.opencirc.api.passport.helper.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;


public class BsddMockStubHelper {

    public static void stubBsddApiResponse() {
        stubFor(get(urlPathEqualTo("/api/Class/v1"))
                .withQueryParam("Uri", equalTo("https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__"))
                .withQueryParam("IncludeClassProperties", equalTo("true"))
                .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
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
                            "classProperties" : [ {
                              "name" : "Handicap Accessible",
                              "uri" : "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__/prop/Pset_SpaceCommon/uri/buildingsmart/ifc/4.3/prop/HandicapAccessible",
                              "dataType" : "Boolean"
                            } ],
                            "definition" : "space designed for human dwelling and related activities",
                            "name" : "Space for human dwelling",
                            "uri" : "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__",
                            "status" : "Active"
                        }
                        """
                        )));
        stubFor(get(urlEqualTo(
                "/uri/etim/etim/10.0/prop/EF000008"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200).withBody(
                                """
                                        {
                                          "connectedPropertyCodes" : [ ],
                                          "dataType" : "Real",
                                          "dimension" : "1 0 0 0 0 0 0",
                                          "dimensionLength" : 1,
                                          "dimensionMass" : 0,
                                          "propertyValueKind" : "Single",
                                          "units" : [ "m", "cm", "mm" ],
                                          "qudtCodes" : [ "M", "CentiM", "MilliM" ],
                                          "dictionaryUri" : "https://identifier.buildingsmart.org/uri/etim/etim/10.0",
                                          "activationDateUtc" : "2025-01-31T00:00:00Z",
                                          "code" : "EF000008",
                                          "countriesOfUse" : [ ],
                                          "definition" : "Overall dimension in the horizontal plane, not applicable for a round product",
                                          "name" : "Width",
                                          "uri" : "https://identifier.buildingsmart.org/uri/etim/etim/10.0/prop/EF000008",
                                          "status" : "Active",
                                          "versionDateUtc" : "2024-12-04T00:00:00Z",
                                        }""")));
    }
}
