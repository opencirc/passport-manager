package com.oc.api.passport.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DataSheet entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataSheetDto {

    /**
     * Unique Id for Datasheet.
     */
    @JsonProperty
    private long datasheetId;

    /**
     * Template entry in JSON format.
     */
    @JsonProperty
    private JsonNode templateEntry;

    /**
     * Data category (Unique or Generic).
     */
    private String dataCategory;

    /**
     * User who created the datasheet.
     */
    private String createdBy;

    /**
     * Time when datasheet is created.
     */
    private LocalDateTime createdTime;
}
