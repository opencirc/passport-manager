package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.model.Admin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Administrator DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AdminDto {

  private String id;

  private String email;

  private String firstName;

  private String lastName;

  private String fullName;

  private boolean active;

  /** Setting up values from AdminPrincipal to AdminDto. */
  public static AdminDto from(AdminPrincipal adminPrincipal) {
    AdminDto adminDto = new AdminDto();
    adminDto.setId(adminPrincipal.getAdminId());
    adminDto.setEmail(adminPrincipal.getEmail());
    adminDto.setFullName(adminPrincipal.getFullName());
    adminDto.setActive(adminPrincipal.isEnabled());
    return adminDto;
  }

  /** Setting up values from Admin to AdminDto. */
  public static AdminDto from(Admin admin) {
    AdminDto adminDto = new AdminDto();
    adminDto.setId(admin.getId());
    adminDto.setEmail(admin.getEmail());
    adminDto.setActive(admin.isActive());
    adminDto.setFirstName(admin.getFirstName());
    adminDto.setLastName(admin.getLastName());
    adminDto.setFullName(admin.getFullName());
    return adminDto;
  }
}
