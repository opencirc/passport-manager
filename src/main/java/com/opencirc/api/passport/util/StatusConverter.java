package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.Passport.Status;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Status, String> {

  @Override
  public String convertToDatabaseColumn(Status status) {
    return status != null ? status.getValue() : null;
  }

  @Override
  public Status convertToEntityAttribute(String dbValue) {
    return Status.fromValue(dbValue);
  }
}
