package com.opencirc.api.passport.util;

import com.opencirc.api.passport.enums.AuditTargetType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditTargetTypeConverter extends GenericEnumConverter<AuditTargetType> {

  /** Creates the converter for the AuditTargetType enum. */
  public AuditTargetTypeConverter() {
    super(AuditTargetType.class);
  }
}
