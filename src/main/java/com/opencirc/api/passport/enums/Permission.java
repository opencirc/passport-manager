package com.opencirc.api.passport.enums;

/** Operation classes an access group can grant to its API keys. */
public enum Permission {
  /** Create child passports and attach datasheets. */
  CREATE,

  /** Query passports, children, and properties. */
  READ,

  /** Modify passport metadata, datasheet properties, and tree associations. */
  UPDATE,

  /** Archive passports or remove datasheets. */
  DELETE
}
