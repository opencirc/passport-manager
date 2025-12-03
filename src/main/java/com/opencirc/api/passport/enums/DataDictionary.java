package com.opencirc.api.passport.enums;

import java.util.Arrays;

/** Enum for Data Dictionary. */
public enum DataDictionary {

  /** IFC. */
  IFC("ifc"),

  /** Lexicon. */
  TABLE6("table6");

  /** Dictionary name in string. */
  private final String value;

  /** Constructor. */
  DataDictionary(String dictionaryValue) {
    this.value = dictionaryValue;
  }

  /** Gets the dictionary value. */
  public String getValue() {
    return value;
  }

  /** Returns the string representation of the enum. */
  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static DataDictionary fromValue(String value) {
    return Arrays.stream(DataDictionary.values())
        .filter(dataDictionary -> dataDictionary.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid dictionary : " + value));
  }
}
