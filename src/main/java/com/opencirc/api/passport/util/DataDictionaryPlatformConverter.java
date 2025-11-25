package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.DataDictionaryPlatform;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DataDictionaryPlatformConverter
    implements AttributeConverter<DataDictionaryPlatform, String> {

  @Override
  public String convertToDatabaseColumn(DataDictionaryPlatform platform) {
    return platform != null ? platform.getValue() : null;
  }

  @Override
  public DataDictionaryPlatform convertToEntityAttribute(String platformValue) {
    return platformValue != null ? DataDictionaryPlatform.fromValue(platformValue) : null;
  }
}
