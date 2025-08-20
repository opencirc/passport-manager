package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatedByDto {

    /**
     * Full name of the user.
     */
    private String fullName;

    /**
     * Email.
     */
    private String email;
}
