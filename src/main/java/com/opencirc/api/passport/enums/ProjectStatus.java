package com.opencirc.api.passport.enums;

import java.util.Arrays;
import lombok.Getter;

/** Operational and archiving status of a project workspace. */
@Getter
public enum ProjectStatus {
  ACTIVE("active"),

  ARCHIVED("archived");

  private final String value;

  ProjectStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static ProjectStatus fromValue(String value) {
    return Arrays.stream(ProjectStatus.values())
        .filter(status -> status.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid project status: " + value));
  }
}
