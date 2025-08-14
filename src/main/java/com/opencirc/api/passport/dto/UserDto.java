package com.opencirc.api.passport.dto;

import java.util.UUID;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDto {

    /**
     * Unique identifier for user.
     */
    private UUID id;

    /**
     * Username.
     */
    private String username;

    /**
     * Email.
     */
    private String email;

    /**
     * Setting up values from UserPrincipal to User Dto.
     *
     * @param userPrincipal
     * @return userDto
     */
    public static UserDto from(UserPrincipal userPrincipal) {
        UserDto userDto = new UserDto();
        userDto.setId(UUID.fromString(userPrincipal.getUserId()));
        userDto.setUsername(userPrincipal.getUsername());
        userDto.setEmail(userPrincipal.getEmail());

        return userDto;
    }

    /**
     * Setting up values from User to User Dto.
     *
     * @param user
     * @return userDto
     */
    public static UserDto from(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

}
