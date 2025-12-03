package com.opencirc.api.passport.util;

import com.opencirc.api.passport.constants.AppConstants;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SecretGenerator {

  /** Allowed character from which random text be generated. */
  private static final String CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789";

  /** Random class instantiation. */
  private static final SecureRandom RANDOM = new SecureRandom();

  /** private constructor to prevents instantiation. */
  private SecretGenerator() {}

  /** Generates a random alphanumeric string. */
  public static String generateRandomString(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Random string length must be greater than 0");
    }
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
    }
    return sb.toString();
  }

  /** Generates an API token with the format: {prefix}_{randomString}_{macHex}. */
  public static String generateApiToken(String prefix, int length, String secretKey) {

    if (prefix == null || prefix.isBlank()) {
      throw new IllegalArgumentException("Prefix must not be null or blank");
    }
    if (length <= 0) {
      throw new IllegalArgumentException("Random string " + "length must be greater than 0");
    }
    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalArgumentException("Secret key must not be null or blank");
    }

    String randomString = generateRandomString(length);
    String macHex =
        computeHmacSha256Hex(
            prefix + "_" + randomString, secretKey, AppConstants.API_KEY_HMAC_HEX_LENGTH);

    return String.format("%s_%s_%s", prefix, randomString, macHex);
  }

  /**
   * Computes HMAC-SHA256(input) using the given secret and returns a hex string, optionally
   * truncated to maxHexLen characters.
   */
  private static String computeHmacSha256Hex(String input, String secretKey, int maxHexLength) {
    Objects.requireNonNull(input, "Input must not be null");
    Objects.requireNonNull(secretKey, "Secret key must not be null");
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] out = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(out.length * 2);
      for (byte b : out) {
        sb.append(String.format("%02x", b));
      }
      if (maxHexLength > 0 && sb.length() > maxHexLength) {
        return sb.substring(0, maxHexLength);
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute token MAC", e);
    }
  }
}
