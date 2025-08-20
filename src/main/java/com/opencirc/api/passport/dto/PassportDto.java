package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
     * Unique Id.
     */
    @JsonProperty
    private String id;

    /**
     * Name of Passport.
     */
    @JsonProperty
    private String name;

    /**
     * Status of Passport.
     */
    @JsonProperty
    private Passport.Status status;

    /**
     * Id of Parent Passport.
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PassportDto parent;


    /**
     * Id of the user who created Passport.
     */
    @JsonProperty
    private String createdById;

    /**
     * Metadata of the user who created Passport.
     */
    @JsonProperty
    private CreatedByDto createdBy;

    /**
     * Time of passport creation.
     */
    @JsonProperty
    private LocalDateTime createdTime;

    /**
     * Linked datasheets.
     */
    private List<DatasheetDto> datasheets;

    /**
     * Setting up values from Passport to Passport Dto.
     * @param passport
     * @return passportDto
     */
    public static PassportDto from(Passport passport) {
        PassportDto dto = new PassportDto();
        dto.setId(passport.getId());
        dto.setName(passport.getName());
        dto.setStatus(passport.getStatus());
        dto.setCreatedById(passport.getCreatedById());
        dto.setCreatedBy(passport.getCreatedBy());
        dto.setCreatedTime(passport.getCreatedTime());

        if (passport.getDatasheetMappings() != null) {
            dto.setDatasheets(passport.getDatasheetMappings().stream()
                    .map(mapping -> DatasheetDto.from(mapping.getDatasheet()))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

}
