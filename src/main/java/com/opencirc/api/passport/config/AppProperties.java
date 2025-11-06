package com.opencirc.api.passport.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

  /** URL for searching class in BsDD. */
  @Value("${bsdd.class.searchText.url}")
  private String bsddClassSearchTextUrl;

  /** URL for fetching class details in BsDD. */
  @Value("${bsdd.classDetails.url}")
  private String bsddClassDetailsUrl;

  /** URL for fetching properties with detail BsDD. */
  @Value("${bsdd.propertiesWithDetail.url}")
  private String bsddPropertiesWithDetailUrl;

  /** URL for text based search in bsdd. */
  @Value("${bsdd.textSearch.url}")
  private String bsddTextSearchUrl;

  /** Access token expiration time. */
  @Value("${jwt.access.token.expiration.time}")
  private String accessTokenExpiryTime;

  /** Refresh token expiration time. */
  @Value("${jwt.refresh.token.expiration.time}")
  private String refreshTokenExpiryTime;

  /** URL for register. */
  @Value("${auth.register.url}")
  private String registerUrl;

  /** URL for login. */
  @Value("${auth.login.url}")
  private String loginUrl;

  /** URL for encryptionKey. */
  @Value("${secret.encryption.key}")
  private String encryptionKey;

  /** Maximum depth of passport hierarchy to create. */
  @Value("${seed.passport.max-level}")
  private String maximumLevel;

  /** Number of child passports to create at each level. */
  @Value("${seed.passport.children-per-level}")
  private String childrenPerLevel;

  /** Number of properties to select from the template. */
  @Value("${seed.passport.properties-count-to-select}")
  private String propertyCountToSelect;

  /** Default Password for the users created by seed. */
  @Value("${seed.user.default-password}")
  private String defaultSeedPassword;

  /** Uri list of passport seed. */
  @Value("${seed.passport.uris}")
  private List<String> uriList;

  /** Path where templates are stored in json format. */
  @Value("${seed.passport.templates.path}")
  private String templatePath;

  /** System admin name. */
  @Value("${system.admin.full-name}")
  private String systemAdminName;

  /** System admin email. */
  @Value("${system.admin.email}")
  private String systemAdminEmail;

  /** Secret key to compute Hmac. */
  @Value("${api.secret.key}")
  private String apiSecretKey;

  /**
   * Getter for refresh token.
   *
   * @return refresh token expiry time
   */
  public int getRefreshTokenExpiryTime() {
    return Integer.parseInt(refreshTokenExpiryTime);
  }

  /**
   * Getter for access token.
   *
   * @return access token expiry time
   */
  public int getAccessTokenExpiryTime() {
    return Integer.parseInt(accessTokenExpiryTime);
  }

  /**
   * Getter for Maximum level.
   *
   * @return Maximum level of the passports to be created
   */
  public int getMaximumLevel() {
    try {
      return Integer.parseInt(maximumLevel);
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid configuration for" + " seed.passport.max-level", e);
    }
  }

  /**
   * Getter for childrenPerLevel.
   *
   * @return Number of children for each passport
   */
  public int getChildrenPerLevel() {
    try {
      return Integer.parseInt(childrenPerLevel);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Invalid configuration for " + "seed.passport.children-per-level", e);
    }
  }

  /**
   * Getter for propertyCountToSelect.
   *
   * @return the number of properties to have in the template
   */
  public int getPropertyCountToSelect() {
    try {
      return Integer.parseInt(propertyCountToSelect);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Invalid configuration for " + "seed.passport.properties-count-to-select", e);
    }
  }
}
