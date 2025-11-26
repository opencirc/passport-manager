package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataDictionaryPlatformConverter extends GenericEnumConverter<DataDictionaryPlatform> {

  /**
   * Creates a DataDictionaryPlatformConverter by passing the DataDictionaryPlatform enum class to
   * the generic converter.
   */
  public DataDictionaryPlatformConverter() {
    super(DataDictionaryPlatform.class);
  }
}
