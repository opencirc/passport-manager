package com.opencirc.api.passport.dto;

import java.time.LocalDateTime;

import com.opencirc.api.passport.model.User;

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

    private LocalDateTime createdTime;
}
