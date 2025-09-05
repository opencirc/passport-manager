package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencirc.api.passport.model.User;

import jakarta.validation.constraints.Email;
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
    @Email
    private String email;

    /**
     * Maps the User to CreatedByDto.
     * @param user
     * @return CreatedByDto
     */
    public static CreatedByDto fromUser(User user) {
        CreatedByDto createdByDto = new CreatedByDto();
        createdByDto.setFullName(user.getFullName() != null
                ? user.getFullName().trim() : null);
        createdByDto.setEmail(user.getEmail());
        return createdByDto;
    }
}
