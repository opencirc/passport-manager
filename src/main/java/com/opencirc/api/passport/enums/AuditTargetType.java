package com.opencirc.api.passport.enums;

import java.util.Arrays;
import lombok.Getter;

/** Entity category audited by an audit log entry. */
@Getter
public enum AuditTargetType {
  PASSPORT("PASSPORT"),

  PASSPORT_DATASHEET("PASSPORT_DATASHEET"),

  DATASHEET_DEFINITION("DATASHEET_DEFINITION");

  private final String value;

  AuditTargetType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static AuditTargetType fromValue(String value) {
    return Arrays.stream(AuditTargetType.values())
        .filter(type -> type.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid audit target type: " + value));
  }
}
