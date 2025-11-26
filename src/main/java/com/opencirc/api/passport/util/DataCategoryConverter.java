package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.Datasheet.DataCategory;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataCategoryConverter extends GenericEnumConverter<DataCategory> {

  /**
   * Creates a DataCategoryConverter by passing the DataCategory enum class to the generic
   * converter.
   */
  public DataCategoryConverter() {
    super(DataCategory.class);
  }
}
