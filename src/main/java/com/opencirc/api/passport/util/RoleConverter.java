package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.User.Role;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter extends GenericEnumConverter<Role> {

  /** Creates a Role by passing the Role enum class to the generic converter. */
  public RoleConverter() {
    super(Role.class);
  }
}
