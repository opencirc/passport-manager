package com.opencirc.api.passport.dto;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BsddClassTemplateDto {

    /**
     * Type of the class.
     */
    @JsonProperty("classType")
    private String classType;

    /**
     * Unique reference code.
     */
    @JsonProperty("referenceCode")
    private String referenceCode;

    /**
     * Names of related IFC entities associated with this class.
     */
    @JsonProperty("relatedIfcEntityNames")
    private List<String> relatedIfcEntityNames;

    /**
     * Reference to the parent class.
     */
    @JsonProperty("parentClassReference")
    private ParentClassReference parentClassReference;

    /**
     * Dynamic set of class properties.
     */
    @JsonProperty("classProperties")
    private JsonNode classProperties;

    /**
     * Hierarchical structure of the class.
     */
    @JsonProperty("hierarchy")
    private List<Hierarchy> hierarchy;


    /**
     * URI of the dictionary.
     */
    @JsonProperty("dictionaryUri")
    private String dictionaryUri;

    /**
     * UTC date when this class became active.
     */
    @JsonProperty("activationDateUtc")
    private OffsetDateTime activationDateUtc;

    /**
     * Code of the class.
     */
    @JsonProperty("code")
    private String code;

    /**
     * Language code.
     */
    @JsonProperty("creatorLanguageCode")
    private String creatorLanguageCode;

    /**
     * List of ISO country codes.
     */
    @JsonProperty("countriesOfUse")
    private List<String> countriesOfUse;

    /**
     * Country code of the origin.
     */
    @JsonProperty("countryOfOrigin")
    private String countryOfOrigin;

    /**
     * Definition of the class.
     */
    @JsonProperty("definition")
    private String definition;

    /**
     * Name of the class.
     */
    @JsonProperty("name")
    private String name;

    /**
     * URI of the class in the dictionary.
     */
    @JsonProperty("uri")
    private String uri;

    /**
     * List of codes for classes that this class replaces.
     */
    @JsonProperty("replacedObjectCodes")
    private List<String> replacedObjectCodes;

    /**
     * List of codes for classes.
     */
    @JsonProperty("replacingObjectCodes")
    private List<String> replacingObjectCodes;

    /**
     * Revision number of the class.
     */
    @JsonProperty("revisionNumber")
    private int revisionNumber;

    /**
     * Status of the class.
     */
    @JsonProperty("status")
    private String status;

    /**
     * List of subdivisions.
     */
    @JsonProperty("subdivisionsOfUse")
    private List<String> subdivisionsOfUse;

    /**
     * Version date.
     */
    @JsonProperty("versionDateUtc")
    private ZonedDateTime versionDateUtc;


    /**
     * Represents the parent class information.
     */
    public static class ParentClassReference {

        /**
         * URI of the parent class.
         */
        @JsonProperty("uri")
        private String uri;

        /**
         * Name of the parent class.
         */
        @JsonProperty("name")
        private String name;

        /**
         * Code of the parent class.
         */
        @JsonProperty("code")
        private String code;

    }

    /**
     * Represents a hierarchical level in the classification system.
     */
    public static class Hierarchy {

        /**
         * Level number.
         */
        @JsonProperty("level")
        private int level;

        /**
         * Name of the node.
         */
        @JsonProperty("name")
        private String name;

        /**
         * Code.
         */
        @JsonProperty("code")
        private String code;

        /**
         * URI of the hierarchy level.
         */
        @JsonProperty("uri")
        private String uri;
    }

}
