package com.opencirc.api.passport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LoginRequestDto {

    /**
     * Username.
     */
    private String username;


    /**
     * password.
     */
    private String password;

}
