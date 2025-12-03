package com.opencirc.api.passport.enums;

import java.util.Arrays;

/** Enum for Data Dictionary platform. */
public enum Platform {

  /** BsDD. */
  BSDD("bsdd"),

  /** Lexicon. */
  LEXICON("lexicon");

  /** Dictionary name in string. */
  private final String value;

  /**
   * Constructor.
   */
  Platform(String dictionaryValue) {
    this.value = dictionaryValue;
  }

  /**
   * Gets the dictionary value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the string representation of the enum.
   */
  @Override
  public String toString() {
    return value;
  }

  /**
   * Parses a string value to its corresponding enum.
   */
  public static Platform fromValue(String value) throws IllegalArgumentException {
    return Arrays.stream(Platform.values())
        .filter(dataDictionary -> dataDictionary.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid dictionary platform: " + value));
  }
}
