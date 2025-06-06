package com.oc.api.passport.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.oc.api.passport.model.Datasheet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Passport entity DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntityDto {

    /**
     * Unique Id
     */
    @JsonProperty
    private String id;

    /**
     * Name
     */
    private String name;

    /**
     * Linked datasheets.
     */
    private List<Datasheet> datasheets;

}
