package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
    private long id;

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
     * User who created the datasheet.
     */
    @JsonProperty
    private String createdBy;

    /**
     * Time when datasheet is created.
     */
    @JsonProperty
    private LocalDateTime createdTime;

    public static DatasheetDto from(Datasheet datasheet) {
        DatasheetDto datasheetDto = new DatasheetDto();
        datasheetDto.id = datasheet.getId();
        datasheetDto.data = datasheet.getData();
        datasheetDto.dataCategory = datasheet.getDataCategory();
        datasheetDto.createdBy = datasheet.getCreatedBy();
        datasheetDto.createdTime = datasheet.getCreatedTime();
        return datasheetDto;
    }
}
