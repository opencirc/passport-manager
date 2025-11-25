package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.Datasheet.DataCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DataCategoryConverter implements AttributeConverter<DataCategory, String> {

  @Override
  public String convertToDatabaseColumn(DataCategory value) {
    return value != null ? value.getValue() : null;
  }

  @Override
  public DataCategory convertToEntityAttribute(String categoryValue) {
    return categoryValue != null ? DataCategory.fromValue(categoryValue) : null;
  }
}
