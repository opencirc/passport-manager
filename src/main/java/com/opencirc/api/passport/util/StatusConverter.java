package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.Passport.Status;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusConverter implements AttributeConverter<Status, String> {

  @Override
  public String convertToDatabaseColumn(Status status) {
    return status != null ? status.getValue() : null;
  }

  @Override
  public Status convertToEntityAttribute(String statusValue) {
    return statusValue != null ? Status.fromValue(statusValue) : null;
  }
}
