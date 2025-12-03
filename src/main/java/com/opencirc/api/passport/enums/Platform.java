package com.opencirc.api.passport.enums;

import java.util.Arrays;
import lombok.Getter;

/** Enum for platforms. */
@Getter
public enum Platform {

  /** BsDD. */
  BSDD("bsdd"),

  /** Lexicon. */
  LEXICON("lexicon");

  /** Dictionary name in string. */
  private final String value;

  /** Constructor. */
  Platform(String dictionaryValue) {
    this.value = dictionaryValue;
  }

  /** Returns the string representation of the enum. */
  @Override
  public String toString() {
    return value;
  }

  /** Parses a string value to its corresponding enum. */
  public static Platform fromValue(String value) throws IllegalArgumentException {
    return Arrays.stream(Platform.values())
        .filter(dataDictionary -> dataDictionary.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid dictionary platform: " + value));
  }
}
