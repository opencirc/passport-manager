package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Datasheet.DataCategory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Datasheet DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DatasheetDto {

    /**
     * Unique Id for Datasheet.
     */
    @JsonProperty
    private String id;

    /**
     * Template information in JSON format.
     */
    @JsonProperty
    private JsonNode data;

    /**
     * Data category (Unique or Generic).
     */
    @JsonProperty
    private DataCategory dataCategory;

    /**
     * Name of the data dictionary from which template is fetched.
     */
    @JsonProperty
    private DataDictionary dataDictionary;

    /**
     * User who created the datasheet.
     */
    @JsonProperty
    private String createdById;

    /**
     * Metadata of the user who created datasheet.
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CreatedByDto createdBy;

    /**
     * Time when datasheet is created.
     */
    @JsonProperty
    private LocalDateTime createdTime;

    /**
     * Maps the Datasheet values to dto.
     * @param datasheet
     * @return datasheetDto
     */
    public static DatasheetDto from(Datasheet datasheet) {
        DatasheetDto datasheetDto = new DatasheetDto();
        datasheetDto.id = String.valueOf(datasheet.getId());
        datasheetDto.data = datasheet.getData();
        datasheetDto.dataCategory = datasheet.getDataCategory();
        datasheetDto.dataDictionary = datasheet.getDataDictionary();
        datasheetDto.createdById = datasheet.getCreatedById();
        datasheetDto.createdBy = datasheet.getCreatedBy();
        datasheetDto.createdTime = datasheet.getCreatedTime();
        return datasheetDto;
    }
}
