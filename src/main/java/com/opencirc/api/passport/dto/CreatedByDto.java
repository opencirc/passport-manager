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

  /** Full name of the user. */
  private String fullName;

  /** Email. */
  @Email private String email;

  /** Maps the UserDto to CreatedByDto. */
  public static CreatedByDto from(UserDto userDto) {
    CreatedByDto dto = new CreatedByDto();
    dto.setFullName(userDto.getFullName());
    dto.setEmail(userDto.getEmail());
    return dto;
  }

  /** Maps the User to CreatedByDto. */
  public static CreatedByDto from(User user) {
    CreatedByDto dto = new CreatedByDto();
    dto.setFullName(user.getFullName());
    dto.setEmail(user.getEmail());
    return dto;
  }
}
