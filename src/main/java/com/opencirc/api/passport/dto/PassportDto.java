package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

        dto.setDatasheets(passport.getDatasheetMappings().stream()
            .map((datasheetMapping) -> DatasheetDto.from(datasheetMapping.getDatasheet()))
            .collect(Collectors.toList()));

        return dto;
    }
}
