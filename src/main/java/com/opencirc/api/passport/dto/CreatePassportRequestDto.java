package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Template Model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class CreatePassportRequestDto {

    /**
     * Formed Template in JSON format.
     */
    @JsonProperty
    private JsonNode dataSheetData;
    
    private String passportName;
    
    private String createdBy;
    
    private LocalDateTime createdTime;
    
}
