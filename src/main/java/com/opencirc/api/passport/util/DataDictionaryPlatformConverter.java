package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.Platform;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataDictionaryPlatformConverter extends GenericEnumConverter<Platform> {

  /**
   * Creates a DataDictionaryPlatformConverter by passing the Platform enum class to the generic
   * converter.
   */
  public DataDictionaryPlatformConverter() {
    super(Platform.class);
  }
}
