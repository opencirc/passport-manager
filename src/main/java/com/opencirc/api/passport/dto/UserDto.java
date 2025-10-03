package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.model.User;
import com.opencirc.api.passport.model.User.Role;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDto {

  /** Unique identifier for user. */
  private UUID id;

  /** Email. */
  private String email;

  /** FirstName of the user.. */
  private String firstName;

  /** LastName of the user.. */
  private String lastName;

  /** FullName of the user. */
  private String fullName;

  /** Role of the user. */
  private Role role;

  /** Holds info if the user is active.. */
  private boolean active;

  /**
   * Setting up values from UserPrincipal to User Dto.
   *
   * @param userPrincipal
   * @return userDto
   */
  public static UserDto from(UserPrincipal userPrincipal) {
    UserDto userDto = new UserDto();
    userDto.setId(UUID.fromString(userPrincipal.getUserId()));
    userDto.setEmail(userPrincipal.getEmail());
    userDto.setFullName(userPrincipal.getFullName());
    userDto.setRole(userPrincipal.getRole());
    userDto.setActive(userPrincipal.isEnabled());
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
