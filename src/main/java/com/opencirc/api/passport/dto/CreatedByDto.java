package com.opencirc.api.passport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencirc.api.passport.model.Admin;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Snapshot of the administrator who created an administrative record. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatedByDto {

  /** Full name of the administrator. */
  private String fullName;

  /** Email. */
  @Email private String email;

  /** Maps the AdminDto to CreatedByDto. */
  public static CreatedByDto from(AdminDto adminDto) {
    CreatedByDto dto = new CreatedByDto();
    dto.setFullName(adminDto.getFullName());
    dto.setEmail(adminDto.getEmail());
    return dto;
  }

  /** Maps the Admin to CreatedByDto. */
  public static CreatedByDto from(Admin admin) {
    CreatedByDto dto = new CreatedByDto();
    dto.setFullName(admin.getFullName());
    dto.setEmail(admin.getEmail());
    return dto;
  }
}
