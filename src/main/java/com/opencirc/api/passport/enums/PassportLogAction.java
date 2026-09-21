package com.opencirc.api.passport.enums;

import lombok.Getter;

/** Enum for Passport Log actions. */
@Getter
public enum PassportLogAction {
  CREATE("CREATE"),
  UPDATE_PROPERTIES("UPDATE_PROPERTIES"),
  ADD_DATASHEET("ADD_DATASHEET"),
  UPDATE_RELATIONSHIPS("UPDATE_RELATIONSHIPS"),
  REMOVE_DATASHEET("REMOVE_DATASHEET"),
  EPD_ENRICHMENT_FAILED("EPD_ENRICHMENT_FAILED");

  private final String value;

  PassportLogAction(String value) {
    this.value = value;
  }
}
