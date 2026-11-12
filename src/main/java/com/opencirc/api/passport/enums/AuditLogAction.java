package com.opencirc.api.passport.enums;

import java.util.Arrays;
import lombok.Getter;

/** High-level mutation recorded by an audit log entry. */
@Getter
public enum AuditLogAction {
  CREATE("CREATE"),

  UPDATE("UPDATE"),

  DELETE("DELETE");

  private final String value;

  AuditLogAction(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static AuditLogAction fromValue(String value) {
    return Arrays.stream(AuditLogAction.values())
        .filter(action -> action.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid audit log action: " + value));
  }
}
