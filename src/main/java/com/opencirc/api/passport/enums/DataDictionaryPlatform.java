package com.opencirc.api.passport.enums;

import java.util.Arrays;

/** Enum for Data Dictionary platform. */
public enum DataDictionaryPlatform {

  /** BsDD. */
  BSDD("bsdd"),

  /** Lexicon. */
  LEXICON("lexicon");

  /** Dictionary name in string. */
  private final String value;

  /**
   * Constructor.
   *
   * @param dictionaryValue the string representation of the dictionary
   */
  DataDictionaryPlatform(String dictionaryValue) {
    this.value = dictionaryValue;
  }

  /**
   * Gets the dictionary value.
   *
   * @return the string representation of the dictionary
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the string representation of the enum.
   *
   * @return the dictionary name as string
   */
  @Override
  public String toString() {
    return value;
  }

  /**
   * Parses a string value to its corresponding enum.
   *
   * @param value the string value to convert
   * @return the corresponding dictionary enum
   * @throws IllegalArgumentException
   */
  public static DataDictionaryPlatform fromValue(String value) {
    return Arrays.stream(DataDictionaryPlatform.values())
        .filter(dataDictionary -> dataDictionary.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid dictionary platform: " + value));
  }
}
