package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataDictionaryPlatformConverter
    implements AttributeConverter<DataDictionaryPlatform, String> {

  @Override
  public String convertToDatabaseColumn(DataDictionaryPlatform platform) {
    return platform != null ? platform.getValue() : null;
  }

  @Override
  public DataDictionaryPlatform convertToEntityAttribute(String dbValue) {
    return DataDictionaryPlatform.fromValue(dbValue);
  }
}
