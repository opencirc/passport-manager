package com.opencirc.api.passport.util;

public final class CommonUtil {

  /** CommonUtil Constructor. */
  private CommonUtil() {}

  /**
   * Converts to lower case.
   *
   * @param input
   * @return lowercased text
   */
  public static String convertToLowercase(String input) {
    if (input == null) {
      return null;
    }
    return input.toLowerCase();
  }
}
