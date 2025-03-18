package com.oc.api.passport.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Passport entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportEntity {

    /**
     * Unique Id for Passport entity.
     */
    @JsonProperty
    private String passportEntityId;

    /**
     * Name of Passport entity.
     */
    private String peName;

    /**
     * Linked datasheets.
     */
    private List<DataSheet> datasheets;

}
