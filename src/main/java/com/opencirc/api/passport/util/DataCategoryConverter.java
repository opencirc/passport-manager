package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.Datasheet.DataCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataCategoryConverter implements AttributeConverter<DataCategory, String> {

  @Override
  public String convertToDatabaseColumn(DataCategory val) {
    return val != null ? val.getValue() : null;
  }

  @Override
  public DataCategory convertToEntityAttribute(String db) {
    return DataCategory.fromValue(db);
  }
}
