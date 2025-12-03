package com.opencirc.api.passport.service;

import com.opencirc.api.passport.config.CompanyConfig;
import com.opencirc.api.passport.constants.AppConstants;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Autowired;

public class CompanyHashService {

  /** Injecting CompanyConfig class. */
  private final CompanyConfig companyConfig;

  /** Company hash value. */
  private String companyHash;

  /** CompanyHashService constructor with config. */
  @Autowired
  public CompanyHashService(CompanyConfig companyConfiguration) {
    this.companyConfig = companyConfiguration;
  }

  /** Computes and stores hash in database. */
  public void computeAndStoreHash() {
    try {
      String base = companyConfig.getName();
      MessageDigest digest = MessageDigest.getInstance(AppConstants.SHA_256);
      byte[] hashBytes = digest.digest(base.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(AppConstants.HEX_STRING & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }

        hexString.append(hex);
      }
      this.companyHash = hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error computing hash", e);
    }
  }

  /** Retrieves the hash. */
  public String getCompanyHash() {
    return companyHash;
  }
}
