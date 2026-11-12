package com.opencirc.api.passport.enums;

import java.util.Arrays;
import lombok.Getter;

/** Validation state of an audit log tag. */
@Getter
public enum ValidationStatus {
  PENDING("pending"),

  APPROVED("approved"),

  REJECTED("rejected");

  private final String value;

  ValidationStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static ValidationStatus fromValue(String value) {
    return Arrays.stream(ValidationStatus.values())
        .filter(status -> status.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid validation status: " + value));
  }
}
