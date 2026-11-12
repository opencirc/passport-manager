package com.opencirc.api.passport.model;

import com.opencirc.api.passport.enums.Permission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CRUD permission profile stored as JSON on an access group. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessGroupPermissions {

  /** Permits creating child passports and attaching datasheets within the project. */
  private boolean canCreate;

  /** Permits querying passports, children, and properties in the project. */
  private boolean canRead;

  /** Permits modifying passport metadata, datasheet properties, and tree associations. */
  private boolean canUpdate;

  /** Permits archiving passports or removing datasheets in the project. */
  private boolean canDelete;

  /** A profile granting every operation. */
  public static AccessGroupPermissions full() {
    return new AccessGroupPermissions(true, true, true, true);
  }

  /** A profile granting read access only. */
  public static AccessGroupPermissions readOnly() {
    return new AccessGroupPermissions(false, true, false, false);
  }

  /** Whether this profile grants the given operation class. */
  public boolean allows(Permission permission) {
    return switch (permission) {
      case CREATE -> canCreate;
      case READ -> canRead;
      case UPDATE -> canUpdate;
      case DELETE -> canDelete;
    };
  }
}
