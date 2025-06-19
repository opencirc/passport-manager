package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RegisterRequestDto {

    /**
     * Username.
     */
    private String username;

    /**
     * Email.
     */
    private String email;

    /**
     * password.
     */
    private String password;

    /**
     * created time.
     */
    private LocalDateTime createdTime;
}
