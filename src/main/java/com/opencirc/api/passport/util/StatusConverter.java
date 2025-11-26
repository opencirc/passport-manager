package com.opencirc.api.passport.util;

import com.opencirc.api.passport.model.Passport.Status;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusConverter extends GenericEnumConverter<Status> {

  /** Creates a StatusConverter by passing the Status enum class to the generic converter. */
  public StatusConverter() {
    super(Status.class);
  }
}
