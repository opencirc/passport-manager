package com.opencirc.api.passport.enums;

import java.util.Arrays;
import lombok.Getter;

/** Operational status of a machine integration credential. */
@Getter
public enum ApiKeyStatus {
  ACTIVE("active"),

  REVOKED("revoked");

  private final String value;

  ApiKeyStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static ApiKeyStatus fromValue(String value) {
    return Arrays.stream(ApiKeyStatus.values())
        .filter(status -> status.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid API key status: " + value));
  }
}
