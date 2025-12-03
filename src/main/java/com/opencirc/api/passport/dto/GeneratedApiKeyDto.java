package com.opencirc.api.passport.dto;

import com.opencirc.api.passport.model.ApiKey;

public class GeneratedApiKeyDto {

  private final ApiKey apiKey;
  private final String rawSecret;

  /** Constructor with parameters. */
  public GeneratedApiKeyDto(ApiKey apiKey, String rawSecret) {
    this.apiKey = apiKey;
    this.rawSecret = rawSecret;
  }

  public ApiKey getApiKey() {
    return apiKey;
  }

  public String getRawSecret() {
    return rawSecret;
  }
}
