package com.opencirc.api.passport.enums;

import java.util.Arrays;

/** Enum for defining the type of a template. */
public enum TemplateType {

  /** class. */
  CLASS("class"),

  /** property. */
  PROPERTY("property");

  /** type in string. */
  private final String value;

  TemplateType(String type) {
    this.value = type;
  }

  /**
   * Parses a string value to its corresponding enum.
   */
  public static TemplateType fromValue(String value) {
    return Arrays.stream(TemplateType.values())
        .filter(type -> type.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + value));
  }

  /**
   * Gets the type value.
   *
   * @return the string representation of the type
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the string representation of the enum.
   *
   * @return the type as string
   */
  @Override
  public String toString() {
    return value;
  }
}
