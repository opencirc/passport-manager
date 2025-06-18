package com.opencirc.api.passport.dto;

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

    @JsonProperty("classType")
    private String classType;

    @JsonProperty("referenceCode")
    private String referenceCode;

    @JsonProperty("relatedIfcEntityNames")
    private List<String> relatedIfcEntityNames;

    @JsonProperty("parentClassReference")
    private ParentClassReference parentClassReference;

    @JsonProperty("classProperties")
    private JsonNode classProperties;

    @JsonProperty("hierarchy")
    private List<Hierarchy> hierarchy;

    @JsonProperty("dictionaryUri")
    private String dictionaryUri;

    @JsonProperty("activationDateUtc")
    private ZonedDateTime activationDateUtc;

    @JsonProperty("code")
    private String code;

    @JsonProperty("creatorLanguageCode")
    private String creatorLanguageCode;

    @JsonProperty("countriesOfUse")
    private List<String> countriesOfUse;

    @JsonProperty("countryOfOrigin")
    private String countryOfOrigin;

    @JsonProperty("definition")
    private String definition;

    @JsonProperty("name")
    private String name;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("replacedObjectCodes")
    private List<String> replacedObjectCodes;

    @JsonProperty("replacingObjectCodes")
    private List<String> replacingObjectCodes;

    @JsonProperty("revisionNumber")
    private int revisionNumber;

    @JsonProperty("status")
    private String status;

    @JsonProperty("subdivisionsOfUse")
    private List<String> subdivisionsOfUse;

    @JsonProperty("versionDateUtc")
    private ZonedDateTime versionDateUtc;

    public static class ParentClassReference {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("name")
        private String name;

        @JsonProperty("code")
        private String code;

    }

    public static class Hierarchy {
        @JsonProperty("level")
        private int level;

        @JsonProperty("name")
        private String name;

        @JsonProperty("code")
        private String code;

        @JsonProperty("uri")
        private String uri;
    }

}
