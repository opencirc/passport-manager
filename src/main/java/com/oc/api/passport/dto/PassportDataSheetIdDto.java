package com.oc.api.passport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for Passport Datasheet Id table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PassportDataSheetIdDto {

    /**
     * Mapping for Passport entity id.
     */
    private String peId;

    /**
     * Mapping for Datasheet id.
     */
    private Long datasheetId;
}
