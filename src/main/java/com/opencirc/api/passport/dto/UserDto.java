package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.auth.principal.UserPrincipal;
import com.opencirc.api.passport.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** UserDto class. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDto {

  private String id;

  private String email;

  private String firstName;

  private String lastName;

  private String fullName;

  private String role;

  private boolean active;

  /** Setting up values from UserPrincipal to User Dto. */
  public static UserDto from(UserPrincipal userPrincipal) {
    UserDto userDto = new UserDto();
    userDto.setId(userPrincipal.getUserId());
    userDto.setEmail(userPrincipal.getEmail());
    userDto.setFullName(userPrincipal.getFullName());
    userDto.setRole(userPrincipal.getRole() != null ? userPrincipal.getRole().getValue() : null);
    userDto.setActive(userPrincipal.isEnabled());
    return userDto;
  }

  /** Setting up values from User to User Dto. */
  public static UserDto from(User user) {
    UserDto userDto = new UserDto();
    userDto.setId(user.getId());
    userDto.setEmail(user.getEmail());
    userDto.setActive(user.isActive());
    userDto.setFirstName(user.getFirstName());
    userDto.setLastName(user.getLastName());
    userDto.setFullName(user.getFullName());
    userDto.setRole(user.getRole() != null ? user.getRole().getValue() : null);
    return userDto;
  }
}
