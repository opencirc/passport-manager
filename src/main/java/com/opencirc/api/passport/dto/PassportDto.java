package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Datasheet.DataCategory;
import com.opencirc.api.passport.model.Passport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Passport DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportDto {

    /**
     * Unique Id
     */
    @JsonProperty
    private String id;

    /**
     * Name of Passport.
     */
    @JsonProperty
    public String name;

    /**
     * Status of Passport.
     */
    @JsonProperty
    public Passport.Status status;

    /**
     * Id of Parent Passport.
     */
    @JsonProperty
    public PassportDto parent;


    /**
     * User who created Passport.
     */
    @JsonProperty
    public String createdBy;

    /**
     * Time of passport creation.
     */
    @JsonProperty
    public LocalDateTime createdTime;

    /**
     * Linked datasheets.
     */
    public List<DatasheetDto> datasheets;

    public static PassportDto from(Passport passport) {
        PassportDto dto = new PassportDto();
        dto.setId(passport.getId());
        dto.setName(passport.getName());
        dto.setStatus(passport.getStatus());

        // @TODO add parent to DTO if it exists

        dto.setCreatedBy(passport.getCreatedBy());
        dto.setCreatedTime(passport.getCreatedTime());

        if (passport.getDatasheetMappings() != null) {
            dto.setDatasheets(passport.getDatasheetMappings().stream()
                .map(mapping -> DatasheetDto.from(mapping.getDatasheet()))
                .collect(Collectors.toList()));
        }

        return dto;
    }
    
    public static PassportDto from(Object[] result) {
        PassportDto dto = new PassportDto();

        dto.setId((String) result[0]);
        dto.setName((String) result[1]);
        dto.setStatus(Passport.Status.valueOf((String) result[5]));
        

        dto.setCreatedBy((String) result[7]);  
        dto.setCreatedTime(((java.sql.Timestamp) result[8]).toLocalDateTime());


        List<DatasheetDto> datasheets = new ArrayList<>();

        DatasheetDto datasheetDto = new DatasheetDto();
        datasheetDto.setId((Long) result[2]);
        datasheetDto.setDataCategory(null); 

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonData = (String) result[3];
            if (jsonData != null) {
                JsonNode jsonNode = mapper.readTree(jsonData);
                datasheetDto.setData(jsonNode);
            }
        } catch (JsonProcessingException e) {
            datasheetDto.setData(null);
        }

        if (result[4] != null) {
            String categoryStr = ((String) result[4]).toUpperCase();
            try {
                datasheetDto.setDataCategory(Datasheet.DataCategory.valueOf(categoryStr));
            } catch (IllegalArgumentException e) {
                datasheetDto.setDataCategory(null);
            }
        } else {
            datasheetDto.setDataCategory(null);
        }

        datasheets.add(datasheetDto);
        dto.setDatasheets(datasheets);

        return dto;

    }
}
