package com.opencirc.api.passport.util;

import com.opencirc.api.passport.exception.InvalidInputException;
import java.util.UUID;

public final class StringUtil {

  /** StringUtil Constructor. */
  private StringUtil() {}

  /**
   * Normalizes the given email by trimming whitespace and converting it to lowercase. Throws an
   * exception if the email is null, blank, or contains invalid characters.
   */
  public static String normalizeEmail(String email) {
    if (email == null) {
      throw new InvalidInputException("Email cannot be null");
    }

    email = email.trim();
    if (email.isEmpty()) {
      throw new InvalidInputException("Email cannot be blank");
    }

    if (email.contains(" ")) {
      throw new InvalidInputException("Email cannot contain spaces");
    }

    return email.toLowerCase();
  }

  /** Safely parses a UUID string. */
  public static UUID validateUuid(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }
    return UUID.fromString(key);
  }
}
