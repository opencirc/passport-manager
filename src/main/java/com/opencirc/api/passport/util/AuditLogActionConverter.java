package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.AuditLogAction;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditLogActionConverter extends GenericEnumConverter<AuditLogAction> {

  /** Creates the converter for the AuditLogAction enum. */
  public AuditLogActionConverter() {
    super(AuditLogAction.class);
  }
}
