package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.DataDictionary;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataDictionaryConverter extends GenericEnumConverter<DataDictionary> {

  /**
   * Creates a DataDictionaryConverter by passing the DataDictionary enum class to the generic
   * converter.
   */
  public DataDictionaryConverter() {
    super(DataDictionary.class);
  }
}
