package com.opencirc.api.passport.config;

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

  @Value("${bsdd.class.searchText.url}")
  private String bsddClassSearchTextUrl;

  @Value("${bsdd.classDetails.url}")
  private String bsddClassDetailsUrl;

  @Value("${bsdd.propertiesWithDetail.url}")
  private String bsddPropertiesWithDetailUrl;

  @Value("${bsdd.textSearch.url}")
  private String bsddTextSearchUrl;

  @Value("${jwt.access.token.expiration.time}")
  private String accessTokenExpiryTime;

  @Value("${jwt.refresh.token.expiration.time}")
  private String refreshTokenExpiryTime;

  @Value("${auth.register.url}")
  private String registerUrl;

  @Value("${auth.login.url}")
  private String loginUrl;

  @Value("${secret.encryption.key}")
  private String encryptionKey;

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
}
