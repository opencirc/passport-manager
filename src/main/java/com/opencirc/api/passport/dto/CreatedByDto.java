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
        if (user == null) {
            return null;
        }
        String name = user.getFullName();
        String email = user.getEmail();
        CreatedByDto dto = new CreatedByDto();
        dto.setFullName(name != null && !name.isBlank() ? name.trim() : null);
        dto.setEmail(email != null ? email.trim() : null);
        return dto;
    }
}
