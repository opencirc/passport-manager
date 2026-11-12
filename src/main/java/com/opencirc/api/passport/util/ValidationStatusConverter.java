package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.ValidationStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ValidationStatusConverter extends GenericEnumConverter<ValidationStatus> {

  /** Creates the converter for the ValidationStatus enum. */
  public ValidationStatusConverter() {
    super(ValidationStatus.class);
  }
}
