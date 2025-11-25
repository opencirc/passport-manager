package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.DataDictionary;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataDictionaryConverter implements AttributeConverter<DataDictionary, String> {

  @Override
  public String convertToDatabaseColumn(DataDictionary dictionary) {
    return dictionary != null ? dictionary.getValue() : null;
  }

  @Override
  public DataDictionary convertToEntityAttribute(String dbValue) {
    return DataDictionary.fromValue(dbValue);
  }
}
