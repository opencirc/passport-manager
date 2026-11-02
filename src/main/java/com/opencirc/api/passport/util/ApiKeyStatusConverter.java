package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.ApiKeyStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApiKeyStatusConverter extends GenericEnumConverter<ApiKeyStatus> {

  /** Creates the converter for the ApiKeyStatus enum. */
  public ApiKeyStatusConverter() {
    super(ApiKeyStatus.class);
  }
}
