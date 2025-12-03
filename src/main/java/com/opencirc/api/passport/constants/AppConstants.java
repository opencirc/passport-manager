package com.opencirc.api.passport.constants;

/** AppConstants. */
public final class AppConstants {

  /** AppConstants Constructor. */
  private AppConstants() {}

  /** Text limit. */
  public static final String QP_BSDD_LIMIT = "limit";

  /** Text SearchText. */
  public static final String QP_BSDD_SEARCHTEXT = "SearchText";

  /** Text TypeFilter. */
  public static final String QP_BSDD_TYPEFILTER = "TypeFilter";

  /** Error message for invalid credentials. */
  public static final String ERR_INVALID_CREDENTIALS = "Invalid Credentials";

  /** Error message for invalid token. */
  public static final String ERR_INVALID_TOKEN = "Invalid token";

  /** Password Strength. */
  public static final int PASSWORD_STRENGTH = 12;

  /** CUID length. */
  public static final int CUID_LENGTH = 36;

  /** Bsdd result limit. */
  public static final int BSDD_LIMIT = 20;

  /** Cors Maximum age. */
  public static final int CORS_MAX_AGE = 25;

  /** hexString. */
  public static final int HEX_STRING = 0xff;

  /** SHA_256. */
  public static final String SHA_256 = "SHA-256";

  /** COOKIE_ACCESS_TOKEN. */
  public static final String COOKIE_ACCESS_TOKEN = "access_token";

  /** COOKIE_REFRESH_TOKEN. */
  public static final String COOKIE_REFRESH_TOKEN = "refresh_token";

  /** API_KEY_RANDOM_STRING_LENGTH. */
  public static final int API_KEY_RANDOM_STRING_LENGTH = 24;

  /** API_KEY_NAME_MAX_LENGTH. */
  public static final int API_KEY_NAME_MAX_LENGTH = 100;

  /** API_KEY_HMAC_HEX_LENGTH. */
  public static final int API_KEY_HMAC_HEX_LENGTH = 12;

  /** HEADER_API_KEY. */
  public static final String HEADER_API_KEY = "X-Api-Key";

  /** HEADER_API_SECRET. */
  public static final String HEADER_API_SECRET = "X-Api-Secret";
}
